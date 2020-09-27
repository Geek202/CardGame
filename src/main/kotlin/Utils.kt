package me.geek.tom.cardgame

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.netty.channel.ChannelHandlerContext
import me.geek.tom.cardgame.protocol.PLAYER_NAME_KEY

val GSON: Gson = GsonBuilder().create()

class GameProtocolError : Exception() { }

fun <T> protocolNotNull(o: T?): T {
    if (o == null) {
        throw GameProtocolError()
    }
    return o
}

fun checkLoggedIn(ctx: ChannelHandlerContext) {
    if (!ctx.channel().hasAttr(PLAYER_NAME_KEY)) // Not logged in!
        throw GameProtocolError()
}
