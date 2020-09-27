package me.geek.tom.cardgame.protocol

import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import me.geek.tom.cardgame.GSON
import me.geek.tom.cardgame.Game

class WebSocketJsonDecoder(private val game: Game) : MessageToMessageDecoder<WebSocketFrame>() {
    override fun decode(ctx: ChannelHandlerContext, frame: WebSocketFrame, out: MutableList<Any>) {
        if (frame is TextWebSocketFrame) {
            try {
                out.add(GSON.fromJson(frame.text(), JsonObject::class.java))
            } catch (e: JsonSyntaxException) {
                ctx.channel().writeAndFlush(CloseWebSocketFrame(WebSocketCloseStatus.INVALID_PAYLOAD_DATA))
            }
        } else {
            ctx.channel().writeAndFlush(CloseWebSocketFrame(WebSocketCloseStatus.INVALID_MESSAGE_TYPE))
        }
    }
}