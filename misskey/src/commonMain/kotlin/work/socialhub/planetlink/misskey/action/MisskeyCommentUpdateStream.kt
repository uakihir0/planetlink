package work.socialhub.planetlink.misskey.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.kmisskey.MisskeyException
import work.socialhub.kmisskey.entity.DeletedNote
import work.socialhub.kmisskey.entity.Reaction as MisskeyReaction
import work.socialhub.kmisskey.stream.MisskeyStream
import work.socialhub.kmisskey.stream.callback.NoteCallback
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.comment.DeleteCommentCallback
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.ErrorCallback
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.misskey.model.MisskeyComment
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.CommentUpdateStream
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.event.IdentifyEvent
import work.socialhub.planetlink.utils.ExceptionHandler

internal class MisskeyCommentUpdateStream(
    private val stream: MisskeyStream,
    private val callback: EventCallback,
    private val service: Service,
    private val host: String,
    private val meId: String?,
) : CommentUpdateStream {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val subscriptionMutex = Mutex()
    private val roots = mutableMapOf<String, MisskeyComment>()
    private val referenceCounts = mutableMapOf<String, Int>()
    private val knownSubscriptions = mutableSetOf<String>()
    private val activeSubscriptions = mutableSetOf<String>()
    private var started = false
    private var callerClosing = false

    override val isOpened: Boolean
        get() = started && stream.isOpen

    private val noteListener = object : NoteCallback {
        override fun onReacted(reaction: MisskeyReaction) {
            scope.launch { handleReaction(reaction, increment = true) }
        }

        override fun onUnreacted(reaction: MisskeyReaction) {
            scope.launch { handleReaction(reaction, increment = false) }
        }

        override fun onNoteDeleted(note: DeletedNote) {
            note.id?.let { scope.launch { handleDeletion(it) } }
        }
    }

    init {
        stream.client.openedCallback = {
            scope.launch {
                subscribeAll()
                (callback as? ConnectCallback)?.onConnect()
            }
        }
        stream.client.closedCallback = {
            scope.launch {
                subscriptionMutex.withLock {
                    activeSubscriptions.clear()
                }
                if (!callerClosing) {
                    (callback as? DisconnectCallback)?.onDisconnect()
                }
                callerClosing = false
            }
        }
        stream.client.errorCallback = { reportError(it) }
    }

    override suspend fun addComments(comments: List<Comment>) {
        val subscribe = mutableSetOf<String>()
        val unsubscribe = mutableSetOf<String>()
        mutex.withLock {
            comments.mapNotNull { it as? MisskeyComment }.forEach { source ->
                val rootId = source.id?.value<String>() ?: return@forEach
                roots.remove(rootId)?.trackedIds()?.forEach { id ->
                    decrementReference(id, unsubscribe)
                }
                val copy = source.copyForUpdate()
                roots[rootId] = copy
                copy.trackedIds().forEach { id ->
                    incrementReference(id, subscribe)
                }
            }
        }
        if (isOpened) {
            unsubscribe.forEach { unsubscribe(it) }
            subscribe.forEach { subscribe(it) }
        }
    }

    override suspend fun removeComments(comments: List<Comment>) {
        val unsubscribe = mutableSetOf<String>()
        mutex.withLock {
            comments.mapNotNull { (it as? MisskeyComment)?.id?.value<String>() }
                .forEach { id ->
                    roots.remove(id)?.trackedIds()?.forEach { trackedId ->
                        decrementReference(trackedId, unsubscribe)
                    }
                }
        }
        if (isOpened) {
            unsubscribe.forEach { unsubscribe(it) }
        }
    }

    override suspend fun open() {
        if (started) return
        started = true
        callerClosing = false
        stream.open()
    }

    override fun close() {
        if (!started) return
        started = false
        callerClosing = true
        stream.close()
        (callback as? DisconnectCallback)?.onDisconnect()
    }

    private suspend fun subscribeAll() {
        val ids = mutex.withLock {
            referenceCounts.filterValues { it > 0 }.keys.toList()
        }
        ids.forEach { subscribe(it) }
    }

    private suspend fun subscribe(id: String) {
        subscriptionMutex.withLock {
            if (!started || !activeSubscriptions.add(id)) return
            if (knownSubscriptions.add(id)) {
                stream.note(id, noteListener)
            } else {
                stream.client.subscribeToNote(
                    id = id,
                    params = null as String?,
                    callbacks = listOf(),
                )
            }
        }
    }

    private suspend fun unsubscribe(id: String) {
        subscriptionMutex.withLock {
            if (!activeSubscriptions.remove(id)) return
            stream.unsubscribe().note(id)
        }
    }

    private fun incrementReference(id: String, subscribe: MutableSet<String>) {
        val count = referenceCounts[id] ?: 0
        referenceCounts[id] = count + 1
        if (count == 0) subscribe.add(id)
    }

    private fun decrementReference(id: String, unsubscribe: MutableSet<String>) {
        val count = referenceCounts[id] ?: return
        if (count <= 1) {
            referenceCounts.remove(id)
            unsubscribe.add(id)
        } else {
            referenceCounts[id] = count - 1
        }
    }

    internal suspend fun handleReaction(
        reaction: MisskeyReaction,
        increment: Boolean,
    ) {
        val noteId = reaction.noteId ?: return
        val reactionName = reaction.reaction ?: return
        try {
            val updates = mutex.withLock {
                roots.mapNotNull { (rootId, root) ->
                    if (noteId !in root.trackedIds()) return@mapNotNull null
                    val updated = root.copyForUpdate()
                    val target = updated.findComment(noteId) ?: return@mapNotNull null
                    target.applyReactionDelta(
                        reactionName = reactionName,
                        userId = reaction.userId,
                        increment = increment,
                    )
                    roots[rootId] = updated
                    updated
                }
            }
            val listener = callback as? UpdateCommentCallback ?: return
            updates.forEach { listener.onUpdate(CommentEvent(it)) }
        } catch (e: Exception) {
            reportError(e)
        }
    }

    internal suspend fun handleDeletion(noteId: String) {
        try {
            val deletedRoots = mutableListOf<String>()
            val updatedRoots = mutableListOf<MisskeyComment>()
            val unsubscribe = mutableSetOf<String>()
            mutex.withLock {
                roots.toMap().forEach { (rootId, root) ->
                    if (rootId == noteId) {
                        roots.remove(rootId)
                        root.trackedIds().forEach { decrementReference(it, unsubscribe) }
                        deletedRoots.add(rootId)
                    } else if (noteId in root.trackedIds()) {
                        val removedIds = root.findComment(noteId)?.trackedIds().orEmpty()
                        val updated = root.copyForUpdate()
                        if (updated.removeNestedComment(noteId)) {
                            roots[rootId] = updated
                            removedIds.forEach { decrementReference(it, unsubscribe) }
                            updatedRoots.add(updated)
                        }
                    }
                }
            }
            if (isOpened) unsubscribe.forEach { unsubscribe(it) }
            val deleteListener = callback as? DeleteCommentCallback
            deletedRoots.forEach { deleteListener?.onDelete(IdentifyEvent(it)) }
            val updateListener = callback as? UpdateCommentCallback
            updatedRoots.forEach { updateListener?.onUpdate(CommentEvent(it)) }
        } catch (e: Exception) {
            reportError(e)
        }
    }

    private fun MisskeyComment.applyReactionDelta(
        reactionName: String,
        userId: String,
        increment: Boolean,
    ) {
        val models = baseReactions().map { it.copyForUpdate() }.toMutableList()
        val existing = models.firstOrNull { it.name == reactionName }
        val isMe = userId == meId

        if (increment) {
            if (existing != null) {
                existing.count = (existing.count ?: 0) + 1
                if (isMe) existing.reacting = true
            } else {
                val mapped = MisskeyMapper.reactions(
                    mapOf(reactionName to 1),
                    reactionName.takeIf { isMe },
                    host,
                )
                models.addAll(mapped)
            }
        } else if (existing != null) {
            existing.count = maxOf((existing.count ?: 0) - 1, 0)
            if (isMe) existing.reacting = false
            if (existing.count == 0 && !existing.reacting) {
                models.remove(existing)
            }
        }
        replaceBaseReactions(models)
    }

    private fun reportError(error: Exception) {
        val listener = callback as? ErrorCallback ?: return
        listener.onError(
            ExceptionHandler.classify(
                error,
                ServiceType.Misskey,
                statusCode = (error as? MisskeyException)?.status,
                responseBody = (error as? MisskeyException)?.body,
            )
        )
    }

    private fun MisskeyComment.findComment(id: String): MisskeyComment? {
        if (this.id?.value<String>() == id) return this
        return (sharedComment as? MisskeyComment)?.findComment(id)
    }

    private fun MisskeyComment.removeNestedComment(id: String): Boolean {
        val shared = sharedComment as? MisskeyComment ?: return false
        if (shared.id?.value<String>() == id) {
            sharedComment = null
            return true
        }
        return shared.removeNestedComment(id)
    }
}

private fun MisskeyComment.trackedIds(): Set<String> {
    val ids = mutableSetOf<String>()
    id?.value<String>()?.let { ids.add(it) }
    (sharedComment as? MisskeyComment)?.let { ids.addAll(it.trackedIds()) }
    return ids
}

private fun MisskeyComment.copyForUpdate(): MisskeyComment {
    return MisskeyComment(service).also { copy ->
        copy.id = id
        copy.text = text
        copy.createAt = createAt
        copy.user = user
        copy.medias = medias
        copy.sharedComment = (sharedComment as? MisskeyComment)?.copyForUpdate()
            ?: sharedComment
        copy.possiblySensitive = possiblySensitive
        copy.application = application
        copy.directMessage = directMessage
        copy.liked = liked
        copy.shared = shared
        copy.likeCount = likeCount
        copy.shareCount = shareCount
        copy.replyTo = replyTo
        copy.requesterHost = requesterHost
        copy.pagingId = pagingId
        copy.spoilerText = spoilerText
        copy.visibility = visibility
        copy.replyCount = replyCount
        copy.poll = poll
        copy.replaceBaseReactions(baseReactions().map { it.copyForUpdate() })
    }
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
