package work.socialhub.planetlink.tumblr

import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.tumblr.expand.AttributedStringEx.tumblr
import kotlin.test.Test

class AttributedStringTest {

    @Test
    fun test() {
        val attr = AttributedString.tumblr(
            // "<figure class=\"tmblr-full\" data-orig-height=\"144\" data-orig-width=\"256\" data-npf='{\"type\":\"video\",\"provider\":\"tumblr\",\"url\":\"https://va.media.tumblr.com/tumblr_scploaFS1V1ypaow4.mp4\",\"media\":{\"url\":\"https://va.media.tumblr.com/tumblr_scploaFS1V1ypaow4.mp4\",\"type\":\"video/mp4\",\"width\":256,\"height\":144},\"poster\":[{\"url\":\"https://64.media.tumblr.com/tumblr_scploaFS1V1ypaow4_frame1.jpg\",\"type\":\"image/jpeg\",\"width\":256,\"height\":144}],\"filmstrip\":{\"url\":\"https://64.media.tumblr.com/previews/tumblr_scploaFS1V1ypaow4_filmstrip.jpg\",\"type\":\"image/jpeg\",\"width\":2000,\"height\":112}}'><video controls=\"controls\" autoplay=\"autoplay\" muted=\"muted\" poster=\"https://64.media.tumblr.com/tumblr_scploaFS1V1ypaow4_frame1.jpg\"><source src=\"https://va.media.tumblr.com/tumblr_scploaFS1V1ypaow4.mp4\" type=\"video/mp4\"></source></video></figure><p>動画テスト</p>"
            "<p><a class=\"tumblr_blog\" href=\"https://viska-chan.tumblr.com/post/190917354148\">viska-chan</a>:</p><blockquote><div class=\"npf_row\"><figure class=\"tmblr-full\" data-orig-height=\"254\" data-orig-width=\"452\"><img src=\"https://64.media.tumblr.com/4120df115bd260fb8ff6bbc6c0abe1d1/9a8996eafb12d2f5-3b/s640x960/13580e62310da8d27fe5e384c4f9ceee0718242b.gif\" data-orig-height=\"254\" data-orig-width=\"452\" srcset=\"https://64.media.tumblr.com/4120df115bd260fb8ff6bbc6c0abe1d1/9a8996eafb12d2f5-3b/s75x75_c1/a1db40e45687de671903decf49467c1ab9667eef.gif 75w, https://64.media.tumblr.com/4120df115bd260fb8ff6bbc6c0abe1d1/9a8996eafb12d2f5-3b/s100x200/f3c315e3848d10f04aec337f284be3dc5050c765.gif 100w, https://64.media.tumblr.com/4120df115bd260fb8ff6bbc6c0abe1d1/9a8996eafb12d2f5-3b/s250x400/54634cd9edf4e1a5ed8799f33d5fd2fbabf39bc8.gif 250w, https://64.media.tumblr.com/4120df115bd260fb8ff6bbc6c0abe1d1/9a8996eafb12d2f5-3b/s400x600/21c918794a034ef8f28023acf35dae606acda404.gif 400w, https://64.media.tumblr.com/4120df115bd260fb8ff6bbc6c0abe1d1/9a8996eafb12d2f5-3b/s500x750/653b60be79e623d9ebd576df9d04fdabef22ac69.gif 452w\" sizes=\"(max-width: 452px) 100vw, 452px\"/></figure></div></blockquote>"
        )

        println()
        println(">> Elements")
        for (element in attr.elements) {
            println("Kind         : ${element.kind}")
            println("DisplayText  : ${element.displayText}")
            println("ExpandedText : ${element.expandedText}")
        }

        println()
        println(">> Display")
        println(attr.displayText)
    }
}