package me.geek.tom.cardgame

import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class CardDeck {

    val cardCount: Int
        get() {
            return cards.size
        }
    private val cards: BlockingQueue<Card> = LinkedBlockingQueue()

    fun shuffle() {
        val cardsCopy = ArrayList(cards)
        cardsCopy.shuffle()
        cards.clear()
        cards.addAll(cardsCopy)
    }

    fun fillDeck() {
        cards.clear()
        for (colour in CardColour.values()) {
            for (value in 1..10) {
                cards.add(Card(colour, value))
            }
        }
    }

    fun drawCard(): Card {
        return cards.poll()
    }

    fun hasCards(): Boolean {
        return cards.isNotEmpty()
    }
}