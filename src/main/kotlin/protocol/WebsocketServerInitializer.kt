package me.geek.tom.cardgame.protocol

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import me.geek.tom.cardgame.Game


class WebsocketServerInitializer(private val game: Game) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()
        pipeline.addLast(HttpServerCodec())
        pipeline.addLast(HttpObjectAggregator(65536))
        pipeline.addLast(WebSocketServerProtocolHandler("/ws", null, true))
        pipeline.addLast(HttpServerHandler())
        pipeline.addLast(WebSocketJsonDecoder(game))
        pipeline.addLast(WebSocketMessageHandler(game))
    }
}