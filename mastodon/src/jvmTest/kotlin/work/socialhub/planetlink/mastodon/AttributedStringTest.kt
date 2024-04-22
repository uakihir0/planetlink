package work.socialhub.planetlink.mastodon

import work.socialhub.planetlink.mastodon.expand.AttributedStringEx.mastodon
import work.socialhub.planetlink.model.common.AttributedString
import kotlin.test.Test

class AttributedStringTest {

    @Test
    fun test() {
        val attr = AttributedString.mastodon(
            //"<p>FEP-2c59: Discovery of a Webfinger address from an ActivityPub actor - Standards / Fediverse Enhancement Proposals - SocialHub<br><a href=\"https://socialhub.activitypub.rocks/t/fep-2c59-discovery-of-a-webfinger-address-from-an-activitypub-actor/3813/47\" rel=\"nofollow noopener noreferrer\" target=\"_blank\"><span class=\"invisible\">https://</span><span class=\"ellipsis\">socialhub.activitypub.rocks/t/</span><span class=\"invisible\">fep-2c59-discovery-of-a-webfinger-address-from-an-activitypub-actor/3813/47</span></a><br>My two cents (?)</p>"
            "<p>SocialHub: Full Stack Developer – Remote/SaaS (m/f/d)</p><p>Headquarters: Schütterlettenweg 4, 85053 Ingolstadt, Deutschland URL: WHO WE ARE We are a Social Media Software as a Service scaleup from Ingolstadt. SocialHub has positioned itself as a remote-first company and achieved the title of OMR market leader in 2023. Our 90-person team is made up of creative personalities and 22 countries. All of us have our own rhythm, but we share the philosophy: &quot;We believe that work…</p><p><a href=\"https://ajtechnicaldr.com/jobs/socialhub-full-stack-developer-remote-saas-m-f-d/\" target=\"_blank\" rel=\"nofollow noopener noreferrer\" translate=\"no\"><span class=\"invisible\">https://</span><span class=\"ellipsis\">ajtechnicaldr.com/jobs/socialh</span><span class=\"invisible\">ub-full-stack-developer-remote-saas-m-f-d/</span></a></p>"
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