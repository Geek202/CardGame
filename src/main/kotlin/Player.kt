package me.geek.tom.cardgame

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.channel.Channel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import me.geek.tom.cardgame.protocol.PLAYER_NAME_KEY

class Player(val name: String, private val game: Game, private val channel: Channel) {
    init {
        channel.attr(PLAYER_NAME_KEY).set(name)
    }

    private val hand: MutableList<Card> = ArrayList()
    var score = 0
        private set

    var currentCard: Int = -1
        private set

    fun heldCard(): Card {
        return hand[currentCard]
    }

    fun sendPacket(msg: Any) {
        channel.writeAndFlush(TextWebSocketFrame(GSON.toJson(msg)))
    }

    fun handlePlayCard(cardNum: Int): Boolean {
        if (currentCard != -1 || cardNum < 0 || cardNum >= hand.size)
            return false

        currentCard = cardNum
        syncData()
        return true
    }

    fun resetAndDrawCards(deck: CardDeck) {
        score = 0
        drawCards(deck, 3)
        syncData()
    }

    fun drawCards(deck: CardDeck, amount: Int) {
        for (i in 1..amount) {
            hand.add(deck.drawCard())
        }
        syncData()
    }

    private fun syncData() {
        val msg = JsonObject()
        val serialisedHand = JsonArray()
        hand.stream().map { c -> c.toJson() }.forEach(serialisedHand::add)
        msg.addProperty("type", "player_sync")
        msg.add("hand", serialisedHand)
        msg.addProperty("score", score)
        msg.addProperty("played_card", currentCard)

        sendPacket(msg)
    }

    fun takeCards(cards: List<Card>) {
        hand.addAll(cards)
    }

    fun takeCurrentCard(): Card {
        val ret = hand.removeAt(currentCard)
        currentCard = -1
        syncData()
        return ret
    }
}