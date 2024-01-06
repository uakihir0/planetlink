package work.socialhub.planetlink.define.emoji

enum class EmojiCategoryType(
    val code: String
) {
    // Auto Generated Code
    // see EmojiGenerator.java (Test)
    Symbols("Symbols"),
    Activities("Activities"),
    Flags("Flags"),
    TravelPlaces("Travel & Places"),
    FoodDrink("Food & Drink"),
    AnimalsNature("Animals & Nature"),
    PeopleBody("People & Body"),
    Objects("Objects"),
    SkinTones("Skin Tones"),
    SmileysEmotion("Smileys & Emotion"),

    /** custom emoji  */
    Custom("Custom"),
    ;

    companion object {
        fun of(code: String): EmojiCategoryType {
            return entries.toTypedArray()
                .first { it.code == code }
        }
    }
}
