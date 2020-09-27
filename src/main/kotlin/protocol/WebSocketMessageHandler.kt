package me.geek.tom.cardgame.protocol

import com.google.gson.JsonObject
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus
import me.geek.tom.cardgame.Game
import me.geek.tom.cardgame.GameProtocolError


class WebSocketMessageHandler(private val game: Game) : SimpleChannelInboundHandler<JsonObject>() {

    override fun channelRead0(ctx: ChannelHandlerContext, obj: JsonObject) {
        try {
            game.onMessage(ctx, obj)
        } catch (e: GameProtocolError) {
            ctx.channel().writeAndFlush(CloseWebSocketFrame(WebSocketCloseStatus.PROTOCOL_ERROR))
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        game.onConnect(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        game.onDisconnect(ctx)
    }
}
