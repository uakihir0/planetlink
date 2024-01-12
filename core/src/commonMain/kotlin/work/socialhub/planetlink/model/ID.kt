package work.socialhub.planetlink.model

import kotlin.jvm.JvmInline

@JvmInline
value class ID(val value: Any) {

    /**
     * Get serialized string ID. (for serialize request)
     * (Get id string with type information.)
     * 識別情報の ID に型情報を入れて文字列として返却
     */
    fun toSerializedString(): String {
        if (value is Int) {
            return "I$value"
        }
        if (value is Long) {
            return "L$value"
        }
        if (value is String) {
            return "S$value"
        }
        throw IllegalStateException("Not supported type.")
    }

    /**
     * Set serialized string ID. (for serialize request)
     * (Set id with typed id string.)
     * 型付き ID 文字列よりID情報を復元
     */

    companion object {
        fun fromSerializedString(value: String): ID {
            if (value.startsWith("I")) {
                return ID(value.substring(1).toInt())
            }
            if (value.startsWith("L")) {
                return ID(value.substring(1).toLong())
            }
            if (value.startsWith("S")) {
                return ID(value.substring(1))
            }
            throw IllegalStateException("Not supported type.")
        }
    }

    /**
     * Is same ID?
     * 同じ識別子か？
     */
    fun isSameID(id: ID): Boolean {
        return (this.value == id.value)
    }

    /**
     * Get typed value.
     * 特定の型に変換して取得
     */
    inline fun <reified T> value(): T {
        if (value is T) return value
        throw IllegalStateException("Not supported type.")
    }
}