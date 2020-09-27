package me.geek.tom.cardgame

import com.google.gson.JsonObject

data class Card(val colour: CardColour, val value: Int) {
    fun toJson(): JsonObject {
        val ret = JsonObject()
        ret.addProperty("colour", colour.name)
        ret.addProperty("value", value)
        return ret
    }

    fun isBetter(other: Card): Boolean {
        return if (other.colour != colour)
            colour.beats(other.colour)
        else {
            value > other.value
        }
    }
}

enum class CardColour {
    BLACK, YELLOW, RED;

    fun beats(other: CardColour): Boolean {
        return when (this) {
            BLACK -> other == RED
            RED -> other == YELLOW
            YELLOW -> other == BLACK
        }
    }
}
