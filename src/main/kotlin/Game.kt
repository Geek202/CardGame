package me.geek.tom.cardgame

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import me.geek.tom.cardgame.protocol.PLAYER_NAME_KEY
import me.geek.tom.cardgame.protocol.WebsocketServerInitializer
import java.util.stream.Collectors


class Game {
    private val playerUsernames: MutableMap<String, Player> = HashMap()
    private val players: MutableList<Player> = ArrayList()
    private var state: GameState = GameState.LOBBY
        set(value) {
            field = value
            notifyStateUpdate()
        }
    private var hostUsername = ""
        set(value) {
            field = value
            notifyStateUpdate()
        }

    // Deck of cards to distribute from
    private var deck = CardDeck()

    // Stores a list of players who have played their cards this round
    private val playedCards: MutableList<String> = ArrayList()

    // Stores a list of cards that are in limbo (ie. were the result of a draw and are up for grabs the next round)
    private val limboCards: MutableList<Card> = ArrayList()

    fun onConnect(ctx: ChannelHandlerContext) {
        println("New connection from: " + ctx.channel().remoteAddress())
    }

    @Suppress("LiftReturnOrAssignment") // for the if statement that looks weird when "improved"
    fun onDisconnect(ctx: ChannelHandlerContext) {
        println("Disconnected: " + ctx.channel().remoteAddress())
        if (ctx.channel().hasAttr(PLAYER_NAME_KEY)) {
            val name = ctx.channel().attr(PLAYER_NAME_KEY).get()

            val player = playerUsernames.remove(name)
            if (player != null)
                players.remove(player)

            if (players.size == 1 && state == GameState.IN_GAME) {
                endGame()
            }

            if (name == hostUsername) { // host has left
                if (players.isNotEmpty()) { // We still have players
                    hostUsername = players[0].name
                } else {
                    // No players left
                    if (state == GameState.IN_GAME) {
                        endGame()
                    }
                    hostUsername = ""
                }
            }
        }
    }

    private fun endGame() {
        if (state != GameState.IN_GAME) return
        state = GameState.ENDED

        val msg = JsonObject()
        msg.addProperty("type", "game_results")
        val leaderboard = JsonArray()
        players.stream().sorted(Comparator.comparing { it.score }).map { it.name }.collect(Collectors.toList())
            .asReversed().forEach<String>(leaderboard::add)
        msg.add("leaderboard", leaderboard)
        broadcastPacket(msg)
    }

    private fun notifyStateUpdate() {
        // reduce memory allocation by defining the object once and reusing in the loop
        val msg = buildStatusPacket()

        broadcastPacket(msg)
    }

    private fun buildStatusPacket(): JsonObject {
        val msg = JsonObject()
        msg.addProperty("type", "game_state")
        msg.addProperty("host", hostUsername)
        msg.addProperty("player_count", players.size)
        msg.addProperty("game_state", state.name)
        val limbo = JsonArray()
        limboCards.stream().map { it.toJson() }.forEach(limbo::add)
        msg.add("limbo_cards", limbo)
        return msg
    }

    private fun broadcastPacket(msg: JsonObject) {
        for (player in players) {
            player.sendPacket(msg)
        }
    }

    fun onMessage(ctx: ChannelHandlerContext, message: JsonObject) {
        when (protocolNotNull(message["type"].asString)) {
            "login" -> handlePlayerLogin(ctx, message)
            "play_card" -> handlePlayCard(ctx, message)
            "start_game" -> handleStartGame(ctx)
            else -> {
                throw GameProtocolError()
            }
        }
    }

    // {"type":"start_game"}
    private fun handleStartGame(ctx: ChannelHandlerContext) {
        checkLoggedIn(ctx)

        val name = ctx.channel().attr(PLAYER_NAME_KEY).get()
        if (state != GameState.LOBBY || name != hostUsername || players.size < 2)
            return

        startGame()
    }

    private fun startGame() {
        deck.fillDeck()
        deck.shuffle()
        for (player in players) {
            player.resetAndDrawCards(deck)
        }
        state = GameState.IN_GAME
        startRound()
    }

    private fun startRound() {
        playedCards.clear()
    }

    // {"type":"play_card","card_num":0}
    private fun handlePlayCard(ctx: ChannelHandlerContext, message: JsonObject) {
        checkLoggedIn(ctx)

        val name = ctx.channel().attr(PLAYER_NAME_KEY).get()
        if (state != GameState.IN_GAME)
            return

        val player = playerUsernames[name]
        val cardNum = message["card_num"].asInt
        if (player?.handlePlayCard(cardNum)!!) {
            playedCards.add(player.name)
        }

        // Everyone has played a card!
        if (playedCards.size == players.size) {
            val winner: Player? = findWinner()
            if (winner != null) {
                val winnerMsg = JsonObject()
                winnerMsg.addProperty("type", "round_end")
                winnerMsg.addProperty("winner", winner.name)
                broadcastPacket(winnerMsg)
                val playedCards: List<Card> = gatherPlayedCards(true)
                winner.takeCards(playedCards)
            } else { // Draw
                limboCards.addAll(gatherPlayedCards(false))
                notifyStateUpdate()
            }

            playedCards.clear()

            if (!deck.hasCards() || deck.cardCount == 1) {
                endGame()
            } else {
                for (p in players) {
                    p.drawCards(deck, 1)
                }
            }
        }
    }

    @Suppress("LiftReturnOrAssignment")
    private fun findWinner(): Player? {
        var currentWinner: Player? = null
        for (player in players) {
            if (currentWinner == null)
                currentWinner = player

            val winningCard = currentWinner.heldCard()
            val currentCard = player.heldCard()
            if (currentCard.isBetter(winningCard)) {
                currentWinner = player
            }
        }
        return currentWinner
    }

    private fun gatherPlayedCards(takeFromLimbo: Boolean): List<Card> {
        val cards: MutableList<Card> = ArrayList()
        for (player in players) {
            cards.add(player.takeCurrentCard())
        }
        if (takeFromLimbo) {
            cards.addAll(limboCards)
            limboCards.clear()
            notifyStateUpdate()
        }
        return cards
    }

    // Receives: {"type":"login","name":"tom"}
    private fun handlePlayerLogin(ctx: ChannelHandlerContext, message: JsonObject) {
        if (state != GameState.LOBBY)
            throw GameProtocolError()

        if (ctx.channel().hasAttr(PLAYER_NAME_KEY)) // Already logged in!
            throw GameProtocolError()

        if (!message.has("name"))
            throw GameProtocolError()

        val name: String? = message["name"].asString
        if (name == null || name.isEmpty() || playerUsernames.keys.contains(name) || players.size >= 2)
            throw GameProtocolError()

        val player = Player(name, this, ctx.channel())

        if (hostUsername == "")
            hostUsername = name

        // Do this after so we don't send the update packet to ourselves
        playerUsernames[name] = player
        players.add(player)

        val res = JsonObject()
        res.addProperty("type", "login")
        res.addProperty("username", name)
        res.addProperty("player_count", players.size)
        res.addProperty("host", hostUsername)

        player.sendPacket(res)

        val updatePacket = buildStatusPacket()
        for (p in players) {
            if (p.name != name) {
                p.sendPacket(updatePacket)
            }
        }
    }
}

enum class GameState {
    LOBBY, IN_GAME, ENDED
}

const val PORT = 7070

fun main() {
    val game = Game()

    val bossGroup: EventLoopGroup = NioEventLoopGroup(1)
    val workerGroup: EventLoopGroup = NioEventLoopGroup()

    try {
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(WebsocketServerInitializer(game))

        val ch: Channel = b.bind(PORT).sync().channel()
        println("Open your web browser and navigate to http://127.0.0.1:$PORT/")
        ch.closeFuture().sync()
    } finally {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }
}
