package work.socialhub.planetlink.tumblr.expand

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import work.socialhub.planetlink.model.common.AttributedItem
import work.socialhub.planetlink.model.common.AttributedKind
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.tumblr.expand.AttributedStringEx.StateType.ByClose
import work.socialhub.planetlink.tumblr.expand.AttributedStringEx.StateType.ByOpen
import work.socialhub.planetlink.tumblr.expand.AttributedStringEx.StateType.Init

object AttributedStringEx {

    enum class StateType {
        Init, ByOpen, ByClose,
    }

    data class Tag(
        val name: String,
        val attributes: Map<String, String>,
        var context: List<Map<String, String>> = listOf(),
    ) {
        operator fun get(text: String): String? {
            return attributes[text]
        }
    }

    data class State(
        val type: StateType,
        val tags: List<Tag>,
        var text: String = "",
    ) {
        fun isVisible(): Boolean {
            return tags.firstOrNull {
                it.attributes["class"]?.contains("invisible") ?: false
            } == null
        }
    }

    fun AttributedString.Companion.tumblr(
        text: String
    ): AttributedString {

        // 初期状態を設定
        val states = mutableListOf<State>()
        states.add(State(Init, listOf()))

        val handler = KsoupHtmlHandler.Builder()
            .onOpenTag { name, attributes, _ ->
                when (name) {

                    // 有効なタグだけを積み上げる
                    "a",
                    "p",
                    "img",
                    "video",
                    "source",
                    "span"
                    -> {
                        val tag = Tag(name, attributes)
                        val tags = (states.last().tags + tag)
                        states.add(State(ByOpen, tags))
                    }
                }
            }
            .onText { t ->
                if (states.isNotEmpty()) {
                    states.last().text += t
                }
            }
            .onCloseTag { name, _ ->
                when (name) {

                    // 有効なタグだけ処理
                    "a",
                    "img",
                    "video",
                    "span"
                    -> {
                        val tags = states.last()
                            .tags.toMutableList()
                            .also { it.removeLast() }
                        states.add(State(ByClose, tags))
                    }

                    // 改行をテキストに追加
                    "br" -> {
                        if (states.isNotEmpty()) {
                            states.last().text += "\n"
                        }
                    }

                    // 改行をテキストに追加
                    "p" -> {
                        val s = states.last()
                        if (s.text.isNotEmpty()) {
                            s.text += "\n\n"
                        }

                        val tags = states.last()
                            .tags.toMutableList()
                            .also { it.removeLast() }
                        states.add(State(ByClose, tags))
                    }

                    "source" -> {
                        val tags = states.last()
                            .tags.toMutableList()
                        val last = tags.removeLast()

                        // Source の属性を一つ下のタグのコンテキストに追加
                        tags.last().context += last.attributes
                        states.add(State(ByClose, tags))
                    }

                    // TODO:
                    // blockquote
                    // iframe
                }
            }
            .onEnd {}
            .build()

        val parser = KsoupHtmlParser(handler)
        parser.write(text)
        parser.end()

        val elements: List<AttributedItem> = states
            .filter { it.tags.isNotEmpty() }
            .filter { it.isVisible() }
            .mapNotNull { s ->
                val tag = s.tags.last()
                val attr = tag.attributes
                val ctx = tag.context

                when (tag.name) {

                    "a" -> {
                        if (s.text.isNotEmpty()) {
                            if (attr["class"]?.contains("hashtag") == true) {
                                AttributedItem().also {
                                    it.kind = AttributedKind.HASH_TAG
                                    it.displayText = s.text
                                    it.expandedText = s.text
                                }

                            } else if (attr["class"]?.contains("u-url") == true) {
                                AttributedItem().also {
                                    it.kind = AttributedKind.ACCOUNT
                                    it.displayText = s.text
                                    it.expandedText = attr["href"]
                                }

                            } else {
                                AttributedItem().also {
                                    it.kind = AttributedKind.LINK
                                    it.displayText = s.text
                                    it.expandedText = attr["href"]
                                }
                            }
                        } else null
                    }

                    "p",
                    "span" -> {
                        // テキスト
                        if (s.text.isNotEmpty()) {

                            // 省略記号を最後に付与
                            if (attr["class"] == "ellipsis") {
                                s.text += "..."
                            }

                            AttributedItem().also {
                                it.kind = AttributedKind.PLAIN
                                it.displayText = s.text
                                it.expandedText = s.text
                            }
                        } else null
                    }

                    "img" -> {
                        if (s.type == ByOpen) {

                            val txt = s.text.ifEmpty {
                                attr["alt"] ?: attr["src"]
                            }

                            AttributedItem().also {
                                it.kind = AttributedKind.IMAGE
                                it.displayText = ""
                                it.expandedText = attr["src"]
                            }
                        } else null
                    }

                    "video" -> {
                        if (s.type == ByOpen) {

                            // コンテキストのソースを確認
                            val v = ctx.firstOrNull {
                                it.containsKey("src")
                            }?.get("src") ?: attr["src"]

                            AttributedItem().also {
                                it.kind = AttributedKind.VIDEO
                                it.displayText = ""
                                it.expandedText = v
                            }
                        } else null
                    }

                    else -> null
                }
            }

        // 最後のテキストエレメントの修正
        if (elements.isNotEmpty()) {
            val last = elements.last()
            if (last.kind == AttributedKind.PLAIN) {
                last.displayText = last.displayText.trimEnd()
                last.expandedText = last.expandedText?.trimEnd()
            }
        }

        return AttributedString(elements)
    }
}