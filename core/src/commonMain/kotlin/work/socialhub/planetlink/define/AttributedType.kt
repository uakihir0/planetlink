package net.socialhub.planetlink.define

import net.socialhub.planetlink.model.common.AttributedKind

object AttributedType {
    // Commons
    val link: net.socialhub.planetlink.model.common.AttributedType =
        CommonAttributedType(AttributedKind.LINK, FULL_URL_REGEX,  //
            { m -> m.group().getDisplayUrl() }, java.util.regex.Matcher::group
        )

    val email: net.socialhub.planetlink.model.common.AttributedType =
        CommonAttributedType(AttributedKind.EMAIL, SIMPLE_EMAIL_REGEX)
    val phone: net.socialhub.planetlink.model.common.AttributedType =
        CommonAttributedType(AttributedKind.PHONE, SIMPLE_PHONE_REGEX)
    val hashTag: net.socialhub.planetlink.model.common.AttributedType =
        CommonAttributedType(AttributedKind.HASH_TAG, HASH_TAG_REGEX)

    // Identify
    val twitter: net.socialhub.planetlink.model.common.AttributedType =
        CommonAttributedType(AttributedKind.ACCOUNT, TWITTER_ACCOUNT_REGEX)
    val mastodon: net.socialhub.planetlink.model.common.AttributedType =
        CommonAttributedType(AttributedKind.ACCOUNT, MASTODON_ACCOUNT_REGEX)

    fun simple(): List<net.socialhub.planetlink.model.common.AttributedType> {
        return java.util.Arrays.asList<net.socialhub.planetlink.model.common.AttributedType>(
            link,
            mastodon,
            email,
            phone,
            hashTag,
            twitter
        )
    }

    object Regex {
        /** URL の正規表現  */
        const val FULL_URL_REGEX: String =
            "(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))"

        /** EMail の簡易的な正規表現  */
        const val SIMPLE_EMAIL_REGEX: String = "([a-zA-Z0-9.!#$%&'*+\\/=?^_`{|}~-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+)"

        /** 電話番号 (国際対応) の正規表現  */
        const val SIMPLE_PHONE_REGEX: String = "([0\\+\\(][\\d\\-\\(\\)]{9,16})"

        /** ハッシュタグ (国際対応) の正規表現  */
        const val HASH_TAG_REGEX: String =
            "([#＃][A-Za-z0-9_À-ÖØ-öø-ÿĀ-ɏɓ-ɔɖ-ɗəɛɣɨɯɲʉʋʻ̀-ͯḀ-ỿЀ-ӿԀ-ԧⷠ-ⷿꙀ-֑ꚟ-ֿׁ-ׂׄ-ׇׅא-תװ-״\uFB12-ﬨשׁ-זּטּ-לּמּנּ-סּףּ-פּצּ-ﭏؐ-ؚؠ-ٟٮ-ۓە-ۜ۞-۪ۨ-ۯۺ-ۼۿݐ-ݿࢠࢢ-ࢬࣤ-ࣾﭐ-ﮱﯓ-ﴽﵐ-ﶏﶒ-ﷇﷰ-ﷻﹰ-ﹴﹶ-ﻼ\u200Cก-ฺเ-๎ᄀ-ᇿ\u3130-ㆅꥠ-\uA97F가-\uD7AFힰ-\uD7FFﾡ-ￜァ-ヺー-ヾｦ-ﾟｰ０-９Ａ-Ｚａ-ｚぁ-ゖ゙-ゞ㐀-\u4DBF一-\u9FFF꜀-뜿띀-렟\uF800-﨟〃々〻]+)"

        /** Mastodon アカウントの正規表現  */
        const val MASTODON_ACCOUNT_REGEX: String = "(@[a-zA-Z0-9_]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)"

        /** Twitter アカウントの正規表現  */
        const val TWITTER_ACCOUNT_REGEX: String = "(@[a-zA-Z0-9_]{2,})"
    }
}
