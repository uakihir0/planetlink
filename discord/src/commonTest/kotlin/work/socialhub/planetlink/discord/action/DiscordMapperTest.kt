package work.socialhub.planetlink.discord.action

import work.socialhub.kdiscord.entity.Attachment
import work.socialhub.kdiscord.entity.ContainerComponent
import work.socialhub.kdiscord.entity.Embed
import work.socialhub.kdiscord.entity.EmbedField
import work.socialhub.kdiscord.entity.EmbedMedia
import work.socialhub.kdiscord.entity.MediaGalleryComponent
import work.socialhub.kdiscord.entity.MediaGalleryItem
import work.socialhub.kdiscord.entity.Message
import work.socialhub.kdiscord.entity.Reaction
import work.socialhub.kdiscord.entity.ReactionCountDetails
import work.socialhub.kdiscord.entity.TextDisplayComponent
import work.socialhub.kdiscord.entity.UnfurledMediaItem
import work.socialhub.kdiscord.entity.User
import work.socialhub.kdiscord.entity.UserPrimaryGuild
import work.socialhub.planetlink.discord.model.DiscordMedia
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.common.AttributedKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DiscordMapperTest {

    private val service = Service("discord", Account())

    @Test
    fun mapsUserMentionUsingGlobalName() {
        val comment = comment(
            content = "Hello,\n<@407874055874543617>!",
            mentions = arrayOf(
                user(
                    id = "407874055874543617",
                    username = "planetlink",
                    globalName = "Planet Link",
                )
            ),
        )

        assertEquals("Hello,\n@Planet Link!", comment.text?.displayText)
        assertEquals(
            listOf(
                AttributedKind.PLAIN,
                AttributedKind.ACCOUNT,
                AttributedKind.PLAIN,
            ),
            comment.text?.elements?.map { it.kind },
        )
        val mention = comment.text?.elements?.single {
            it.kind == AttributedKind.ACCOUNT
        }
        assertEquals("@Planet Link", mention?.displayText)
        assertEquals("407874055874543617", mention?.expandedText)
    }

    @Test
    fun mapsLegacyMentionUsingUsernameWhenGlobalNameIsMissing() {
        val comment = comment(
            content = "<@!407874055874543617>",
            mentions = arrayOf(
                user(
                    id = "407874055874543617",
                    username = "planetlink",
                )
            ),
        )

        assertEquals("@planetlink", comment.text?.displayText)
        assertEquals(
            AttributedKind.ACCOUNT,
            comment.text?.elements?.single()?.kind,
        )
    }

    @Test
    fun mapsMultipleAndRepeatedMentions() {
        val comment = comment(
            content = "<@1> and <@2>, again <@1>",
            mentions = arrayOf(
                user(id = "1", username = "one", globalName = "One"),
                user(id = "2", username = "two", globalName = "Two"),
            ),
        )

        assertEquals("@One and @Two, again @One", comment.text?.displayText)
        assertEquals(
            listOf("@One", "@Two", "@One"),
            comment.text?.elements
                ?.filter { it.kind == AttributedKind.ACCOUNT }
                ?.map { it.displayText },
        )
    }

    @Test
    fun preservesUnknownMention() {
        val comment = comment(
            content = "Hello <@999> and <@1>",
            mentions = arrayOf(
                user(id = "1", username = "one", globalName = "One")
            ),
        )

        assertEquals("Hello <@999> and @One", comment.text?.displayText)
        assertEquals(
            listOf("@One"),
            comment.text?.elements
                ?.filter { it.kind == AttributedKind.ACCOUNT }
                ?.map { it.displayText },
        )
    }

    @Test
    fun preservesRichTextParsingAroundMention() {
        val comment = comment(
            content = "See https://example.com/docs and <@1> #updates",
            mentions = arrayOf(
                user(id = "1", username = "one", globalName = "One")
            ),
        )

        assertEquals(
            "See https://example.com/docs and @One #updates",
            comment.text?.displayText,
        )
        assertEquals(
            listOf(
                AttributedKind.PLAIN,
                AttributedKind.LINK,
                AttributedKind.PLAIN,
                AttributedKind.ACCOUNT,
                AttributedKind.PLAIN,
                AttributedKind.HASH_TAG,
                AttributedKind.PLAIN,
            ),
            comment.text?.elements?.map { it.kind },
        )
    }

    @Test
    fun mapsRichWebhookEmbedIntoUnifiedComment() {
        val message = Message().also {
            it.id = "1533351697358000172"
            it.channelId = "1520652950551138409"
            it.content = ""
            it.editedTimestamp = "2026-08-02T06:00:00Z"
            it.webhookId = "1520652982985691219"
            it.flags = 0
            it.pinned = true
            it.author = user(
                id = "1520652982985691219",
                username = "FeedBack",
                globalName = null,
            ).also { author ->
                author.primaryGuild = UserPrimaryGuild().also { guild ->
                    guild.identityGuildId = "guild-id"
                    guild.identityEnabled = true
                    guild.tag = "PL"
                }
            }
            it.embeds = arrayOf(
                Embed().also { embed ->
                    embed.type = "rich"
                    embed.title = "New Feedback"
                    embed.description = "Feedback body"
                    embed.color = 6514417
                    embed.contentScanVersion = 4
                    embed.fields = arrayOf(
                        EmbedField().also { field ->
                            field.name = "Build"
                            field.value = "af4b825"
                            field.inline = true
                        },
                        EmbedField().also { field ->
                            field.name = "Locale"
                            field.value = "ja"
                            field.inline = true
                        },
                    )
                    embed.image = EmbedMedia().also { image ->
                        image.url = "https://cdn.example.com/feedback.png"
                        image.proxyUrl = "https://media.example.com/feedback.png"
                        image.contentType = "image/png"
                        image.width = 1206
                        image.height = 2622
                        image.placeholder = "BQgCAw=="
                        image.placeholderVersion = 1
                    }
                }
            )
        }

        val comment = DiscordMapper.comment(message, null, service)

        assertEquals(
            "New Feedback\n\nFeedback body\n\nBuild\naf4b825\n\nLocale\nja",
            comment.text?.displayText,
        )
        assertEquals("1520652982985691219", comment.webhookId)
        assertEquals(0, comment.messageFlags)
        assertTrue(comment.pinned)
        assertEquals("PL", (comment.user as? work.socialhub.planetlink.discord.model.DiscordUser)
            ?.primaryGuild?.tag)
        val embed = comment.embeds.single()
        assertEquals("rich", embed.type)
        assertEquals("New Feedback", embed.title)
        assertEquals("Feedback body", embed.description)
        assertEquals(6514417, embed.color)
        assertEquals(4, embed.contentScanVersion)
        assertEquals("af4b825", embed.fields.first().value?.displayText)

        val media = assertIs<DiscordMedia>(comment.medias.single())
        assertEquals("https://cdn.example.com/feedback.png", media.sourceUrl)
        assertEquals("https://media.example.com/feedback.png", media.previewUrl)
        assertEquals(1206, media.width)
        assertEquals(2622, media.height)
        assertEquals("BQgCAw==", media.placeholder)
    }

    @Test
    fun mapsComponentTextAndGalleryMedia() {
        val message = Message().also {
            it.content = "Message content"
            it.components = arrayOf(
                ContainerComponent().also { container ->
                    container.components = arrayOf(
                        TextDisplayComponent().also { text ->
                            text.content = "Component content"
                        },
                        MediaGalleryComponent().also { gallery ->
                            gallery.items = arrayOf(
                                MediaGalleryItem().also { item ->
                                    item.description = "Gallery image"
                                    item.media = UnfurledMediaItem().also { media ->
                                        media.url = "https://cdn.example.com/gallery.png"
                                        media.contentType = "image/png"
                                        media.placeholder = "AQIDBA=="
                                    }
                                }
                            )
                        },
                    )
                }
            )
        }

        val comment = DiscordMapper.comment(message, null, service)

        assertEquals(
            "Message content\n\nComponent content\n\nGallery image",
            comment.text?.displayText,
        )
        assertEquals(1, comment.components.size)
        assertEquals(2, comment.components.single().children.size)
        assertEquals("https://cdn.example.com/gallery.png", comment.medias.single().sourceUrl)
        assertEquals("Gallery image", comment.medias.single().description)
    }

    @Test
    fun excludesSpoilerComponentTextAndPropagatesSpoilerToMedia() {
        val message = Message().also {
            it.content = "Visible content"
            it.components = arrayOf(
                ContainerComponent().also { container ->
                    container.spoiler = true
                    container.components = arrayOf(
                        TextDisplayComponent().also { text ->
                            text.content = "Hidden content"
                        },
                        MediaGalleryComponent().also { gallery ->
                            gallery.items = arrayOf(
                                MediaGalleryItem().also { item ->
                                    item.description = "Hidden image"
                                    item.media = UnfurledMediaItem().also { media ->
                                        media.url = "https://cdn.example.com/hidden.png"
                                        media.contentType = "image/png"
                                    }
                                }
                            )
                        },
                    )
                }
            )
        }

        val comment = DiscordMapper.comment(message, null, service)

        assertEquals("Visible content", comment.text?.displayText)
        assertTrue(assertIs<DiscordMedia>(comment.medias.single()).spoiler)
        assertTrue(
            assertIs<DiscordMedia>(
                comment.components.single().children[1].medias.single()
            ).spoiler
        )
    }

    @Test
    fun excludesSpoilerGalleryItemDescriptionFromText() {
        val message = Message().also {
            it.content = "Message content"
            it.components = arrayOf(
                MediaGalleryComponent().also { gallery ->
                    gallery.items = arrayOf(
                        MediaGalleryItem().also { item ->
                            item.description = "Visible image"
                            item.media = UnfurledMediaItem().also { media ->
                                media.url = "https://cdn.example.com/visible.png"
                                media.contentType = "image/png"
                            }
                        },
                        MediaGalleryItem().also { item ->
                            item.description = "Hidden image"
                            item.spoiler = true
                            item.media = UnfurledMediaItem().also { media ->
                                media.url = "https://cdn.example.com/hidden.png"
                                media.contentType = "image/png"
                            }
                        },
                    )
                }
            )
        }

        val comment = DiscordMapper.comment(message, null, service)

        assertEquals("Message content\n\nVisible image", comment.text?.displayText)
        assertEquals(
            listOf("Visible image"),
            comment.components.single().text?.displayText?.lines(),
        )
        val media = comment.medias.single { it.sourceUrl == "https://cdn.example.com/hidden.png" }
        assertTrue(assertIs<DiscordMedia>(media).spoiler)
    }

    @Test
    fun marksSpoilerAttachmentFromFilename() {
        val message = Message().also {
            it.content = "Message content"
            it.attachments = arrayOf(
                Attachment().also { attachment ->
                    attachment.id = "1"
                    attachment.filename = "SPOILER_secret.png"
                    attachment.url = "https://cdn.example.com/secret.png"
                    attachment.contentType = "image/png"
                }
            )
        }

        val comment = DiscordMapper.comment(message, null, service)

        assertTrue(assertIs<DiscordMedia>(comment.medias.single()).spoiler)
    }

    @Test
    fun preservesGalleryItemsWithSharedUrls() {
        val sharedUrl = "https://cdn.example.com/shared.png"
        val message = Message().also {
            it.components = arrayOf(
                MediaGalleryComponent().also { gallery ->
                    gallery.items = arrayOf(
                        MediaGalleryItem().also { item ->
                            item.description = "First image"
                            item.media = UnfurledMediaItem().also { media ->
                                media.url = sharedUrl
                                media.contentType = "image/png"
                            }
                        },
                        MediaGalleryItem().also { item ->
                            item.description = "Second image"
                            item.media = UnfurledMediaItem().also { media ->
                                media.url = sharedUrl
                                media.contentType = "image/png"
                            }
                        },
                    )
                }
            )
        }

        val comment = DiscordMapper.comment(message, null, service)

        assertEquals(2, comment.medias.size)
        assertEquals(
            listOf(sharedUrl, sharedUrl),
            comment.medias.map { it.sourceUrl },
        )
        assertEquals(
            listOf("First image", "Second image"),
            comment.medias.map { it.description },
        )
    }

    @Test
    fun mapsNormalAndBurstReactionDetails() {
        val message = Message().also {
            it.reactions = arrayOf(
                Reaction().also { reaction ->
                    reaction.count = 3
                    reaction.me = false
                    reaction.meBurst = true
                    reaction.burstColors = arrayOf("#5865F2")
                    reaction.countDetails = ReactionCountDetails().also { details ->
                        details.normal = 2
                        details.burst = 1
                    }
                    reaction.emoji = work.socialhub.kdiscord.entity.Emoji().also { emoji ->
                        emoji.name = "🫨"
                    }
                }
            )
        }

        val comment = DiscordMapper.comment(message, null, service)

        assertTrue(comment.reactions.single().reacting)
        assertEquals(3, comment.reactions.single().count)
        assertEquals(2, comment.reactionDetails.single().normalCount)
        assertEquals(1, comment.reactionDetails.single().burstCount)
        assertEquals(listOf("#5865F2"), comment.reactionDetails.single().burstColors)
        assertTrue(comment.reactionDetails.single().reactingWithBurst)
    }

    private fun comment(
        content: String,
        mentions: Array<User>,
    ) = DiscordMapper.comment(
        Message().also {
            it.content = content
            it.mentions = mentions
        },
        userMe = null,
        service = service,
    )

    private fun user(
        id: String,
        username: String,
        globalName: String? = null,
    ) = User().also {
        it.id = id
        it.username = username
        it.globalName = globalName
    }
}
