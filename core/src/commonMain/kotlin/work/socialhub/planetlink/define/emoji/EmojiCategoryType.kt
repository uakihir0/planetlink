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
    SmileysEmotion("Smileys & Emotion"),  //endregion

    /** custom emoji  */
    Custom("Custom"),

    companion object {
        fun of(code: String): EmojiCategoryType {
            return java.util.stream.Stream.of(*entries.toTypedArray()) //
                .filter(java.util.function.Predicate<EmojiCategoryType> { e: EmojiCategoryType -> e.code == code }) //
                .findFirst().orElse(null)
        }
    }
}
