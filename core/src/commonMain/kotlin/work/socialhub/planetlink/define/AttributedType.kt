package work.socialhub.planetlink.define

import work.socialhub.planetlink.define.AttributedType.Regex.FULL_URL_REGEX
import work.socialhub.planetlink.define.AttributedType.Regex.HASH_TAG_REGEX
import work.socialhub.planetlink.define.AttributedType.Regex.SIMPLE_EMAIL_REGEX
import work.socialhub.planetlink.define.AttributedType.Regex.SIMPLE_PHONE_REGEX
import work.socialhub.planetlink.model.common.AttributedKind
import work.socialhub.planetlink.model.common.AttributedType
import work.socialhub.planetlink.model.common.AttributedType.CommonAttributedType

object AttributedType {

    // Commons
    val link: AttributedType =
        CommonAttributedType(
            AttributedKind.LINK, FULL_URL_REGEX,
            { it.value },
            { it.value }
        )

    val email: AttributedType =
        CommonAttributedType(
            AttributedKind.EMAIL,
            SIMPLE_EMAIL_REGEX
        )

    val phone: AttributedType =
        CommonAttributedType(
            AttributedKind.PHONE,
            SIMPLE_PHONE_REGEX
        )

    val hashTag: AttributedType =
        CommonAttributedType(
            AttributedKind.HASH_TAG,
            HASH_TAG_REGEX
        )

    fun simple(): List<AttributedType> {
        return listOf(
            link,
            email,
            phone,
            hashTag,
        )
    }

    object Regex {
        /** URL の正規表現  */
        val FULL_URL_REGEX = //
            "(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))".toRegex()

        /** EMail の簡易的な正規表現  */
        val SIMPLE_EMAIL_REGEX = //
            "([a-zA-Z0-9.!#$%&'*+\\/=?^_`{|}~-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+)".toRegex()

        /** 電話番号 (国際対応) の正規表現  */
        val SIMPLE_PHONE_REGEX = //
            "([0\\+\\(][\\d\\-\\(\\)]{9,16})".toRegex()

        /** ハッシュタグ (国際対応) の正規表現  */
        val HASH_TAG_REGEX =
            "([#＃][A-Za-z0-9_À-ÖØ-öø-ÿĀ-ɏɓ-ɔɖ-ɗəɛɣɨɯɲʉʋʻ̀-ͯḀ-ỿЀ-ӿԀ-ԧⷠ-ⷿꙀ-֑ꚟ-ֿׁ-ׂׄ-ׇׅא-תװ-״\uFB12-ﬨשׁ-זּטּ-לּמּנּ-סּףּ-פּצּ-ﭏؐ-ؚؠ-ٟٮ-ۓە-ۜ۞-۪ۨ-ۯۺ-ۼۿݐ-ݿࢠࢢ-ࢬࣤ-ࣾﭐ-ﮱﯓ-ﴽﵐ-ﶏﶒ-ﷇﷰ-ﷻﹰ-ﹴﹶ-ﻼ\u200Cก-ฺเ-๎ᄀ-ᇿ\u3130-ㆅꥠ-\uA97F가-\uD7AFힰ-\uD7FFﾡ-ￜァ-ヺー-ヾｦ-ﾟｰ０-９Ａ-Ｚａ-ｚぁ-ゖ゙-ゞ㐀-\u4DBF一-\u9FFF꜀-뜿띀-렟\uF800-﨟〃々〻]+)".toRegex()

        /** Mastodon アカウントの正規表現  */
        val MASTODON_ACCOUNT_REGEX =
            "(@[a-zA-Z0-9_]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)".toRegex()

        /** X アカウントの正規表現  */
        val X_ACCOUNT_REGEX =
            "(@[a-zA-Z0-9_]{2,})".toRegex()
    }
}
