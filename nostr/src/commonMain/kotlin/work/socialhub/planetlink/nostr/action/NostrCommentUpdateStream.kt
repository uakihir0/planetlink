package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.knostr.social.api.EnrichmentResource
import work.socialhub.knostr.social.model.NostrNote
import work.socialhub.knostr.social.model.NostrNoteStats
import work.socialhub.knostr.social.model.SocialDataBatch
import work.socialhub.knostr.social.model.SocialDataRequest
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.ErrorCallback
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.CommentUpdateStream
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.nostr.model.NostrComment
import work.socialhub.planetlink.utils.ExceptionHandler

internal class NostrEnrichmentDispatcher(
    private val enrichment: EnrichmentResource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val listeners = mutableSetOf<(SocialDataBatch) -> Unit>()
    private var previousCallback: ((SocialDataBatch) -> Unit)? = null

    private val dispatchCallback: (SocialDataBatch) -> Unit = { batch ->
        scope.launch {
            val previous = mutex.withLock { previousCallback }
            try {
                previous?.invoke(batch)
            } catch (_: Exception) {
            }
            val snapshot = mutex.withLock { listeners.toList() }
            snapshot.forEach { listener ->
                try {
                    listener(batch)
                } catch (_: Exception) {
                }
            }
        }
    }

    suspend fun register(listener: (SocialDataBatch) -> Unit) {
        mutex.withLock {
            if (listeners.isEmpty()) {
                previousCallback = enrichment.onUpdateCallback
                enrichment.onUpdateCallback = dispatchCallback
            }
            listeners.add(listener)
        }
    }

    suspend fun unregister(listener: (SocialDataBatch) -> Unit) {
        mutex.withLock {
            listeners.remove(listener)
            if (listeners.isEmpty() && enrichment.onUpdateCallback === dispatchCallback) {
                enrichment.onUpdateCallback = previousCallback
                previousCallback = null
            }
        }
    }
}

internal class NostrCommentUpdateStream(
    private val enrichment: EnrichmentResource,
    private val dispatcher: NostrEnrichmentDispatcher,
    private val callback: EventCallback,
    private val service: Service,
    private val userMe: User?,
) : CommentUpdateStream {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val roots = mutableMapOf<String, NostrComment>()
    private var opened = false
    private var unregisterJob: Job? = null

    override val isOpened: Boolean
        get() = opened

    private val enrichmentListener: (SocialDataBatch) -> Unit = { batch ->
        if (opened) {
            scope.launch {
                applyBatch(batch)
            }
        }
    }

    override suspend fun addComments(comments: List<Comment>) {
        val added = mutex.withLock {
            comments.mapNotNull { it as? NostrComment }
                .mapNotNull { comment ->
                    comment.id?.value<String>()?.let { it to comment.copyForUpdate() }
                }
                .also { entries ->
                    entries.forEach { (id, comment) -> roots[id] = comment }
                }
                .map { it.second }
        }
        if (opened && added.isNotEmpty()) {
            requestUpdates(added)
        }
    }

    override suspend fun removeComments(comments: List<Comment>) {
        val ids = comments.mapNotNull {
            (it as? NostrComment)?.id?.value<String>()
        }.toSet()
        mutex.withLock {
            roots.keys.removeAll(ids)
        }
    }

    override suspend fun open() {
        if (opened) return
        unregisterJob?.join()
        unregisterJob = null
        opened = true
        dispatcher.register(enrichmentListener)
        val comments = mutex.withLock { roots.values.toList() }
        requestUpdates(comments)
        (callback as? ConnectCallback)?.onConnect()
    }

    override fun close() {
        if (!opened) return
        opened = false
        unregisterJob = scope.launch {
            dispatcher.unregister(enrichmentListener)
        }
        (callback as? DisconnectCallback)?.onDisconnect()
    }

    private fun requestUpdates(comments: List<NostrComment>) {
        val allComments = comments.flatMap { it.flatten() }
        enrichment.request(
            SocialDataRequest(
                userPubkeys = allComments.flatMap { comment ->
                    listOfNotNull(
                        comment.user?.id?.value<String>(),
                        comment.authorPubkey,
                    )
                }.distinct(),
                noteIds = allComments.mapNotNull { comment ->
                    comment.quotedEventId?.takeIf { comment.sharedComment == null }
                }.distinct(),
                noteStatsEventIds = allComments.mapNotNull {
                    it.id?.value<String>()
                }.distinct(),
            ),
            forceRefresh = false,
        )
    }

    private suspend fun applyBatch(batch: SocialDataBatch) {
        try {
            val updates = mutex.withLock {
                roots.mapNotNull { (id, root) ->
                    val updated = root.copyForUpdate()
                    if (updated.applyBatch(batch)) {
                        roots[id] = updated
                        updated
                    } else {
                        null
                    }
                }
            }
            if (!opened) return
            val listener = callback as? UpdateCommentCallback ?: return
            updates.forEach { listener.onUpdate(CommentEvent(it)) }
        } catch (e: Exception) {
            val listener = callback as? ErrorCallback ?: return
            listener.onError(ExceptionHandler.classify(e, ServiceType.Nostr))
        }
    }

    private fun NostrComment.applyBatch(batch: SocialDataBatch): Boolean {
        var changed = false

        batch.notes.forEach { note ->
            if (applyNote(note)) changed = true
        }

        val users = batch.users.associateBy { it.pubkey }
        flatten().forEach { comment ->
            val pubkey = comment.user?.id?.value<String>()
            val user = pubkey?.let { users[it] }
            if (user != null) {
                comment.user = NostrMapper.user(user, service)
                changed = true
            }
        }

        val stats = batch.noteStats.associateBy { it.eventId }
        flatten().forEach { comment ->
            val update = comment.id?.value<String>()?.let { stats[it] }
            if (update != null) {
                comment.applyStats(update)
                changed = true
            }
        }
        return changed
    }

    private fun NostrComment.applyNote(note: NostrNote): Boolean {
        if (id?.value<String>() == note.event.id) {
            copyFrom(NostrMapper.comment(note, service, userMe))
            return true
        }
        if (quotedEventId == note.event.id) {
            sharedComment = NostrMapper.comment(note, service, userMe)
            text = text?.displayText?.let { content ->
                AttributedString.plain(NostrMapper.stripQuoteReference(content, note.event.id))
            }
            return true
        }
        return (sharedComment as? NostrComment)?.applyNote(note) == true
    }

    private fun NostrComment.applyStats(stats: NostrNoteStats) {
        likeCount = stats.likeCount
        replyCount = stats.replyCount
        repostCount = stats.repostCount
        reactions = reactions.withCount("like", stats.likeCount)
            .withCount("repost", stats.repostCount)
    }

    private fun List<Reaction>.withCount(name: String, count: Int): List<Reaction> {
        val models = map { it.copyForUpdate() }.toMutableList()
        val existing = models.firstOrNull { it.name == name }
        if (existing != null) {
            existing.count = count
            if (count == 0 && !existing.reacting) models.remove(existing)
        } else if (count > 0) {
            models.add(Reaction().also {
                it.name = name
                it.count = count
            })
        }
        return models
    }
}

private fun NostrComment.flatten(): List<NostrComment> {
    val comments = mutableListOf(this)
    (sharedComment as? NostrComment)?.let { comments.addAll(it.flatten()) }
    return comments
}

private fun NostrComment.copyForUpdate(): NostrComment {
    return NostrComment(service).also { copy ->
        copy.copyFrom(this)
    }
}

private fun NostrComment.copyFrom(source: NostrComment) {
    id = source.id
    eventId = source.eventId
    quotedEventId = source.quotedEventId
    replyCount = source.replyCount
    likeCount = source.likeCount
    repostCount = source.repostCount
    contentWarning = source.contentWarning
    channelId = source.channelId
    authorPubkey = source.authorPubkey
    text = source.text
    createAt = source.createAt
    user = source.user
    medias = source.medias
    sharedComment = (source.sharedComment as? NostrComment)?.copyForUpdate()
        ?: source.sharedComment
    possiblySensitive = source.possiblySensitive
    application = source.application
    directMessage = source.directMessage
    reactions = source.reactions.map { it.copyForUpdate() }
}

private fun Reaction.copyForUpdate(): Reaction {
    return Reaction().also {
        it.name = name
        it.emoji = emoji
        it.iconUrl = iconUrl
        it.count = count
        it.reacting = reacting
    }
}
