package me.geek.tom.cardgame.protocol

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import io.netty.handler.codec.http.HttpMethod.GET
import io.netty.handler.codec.http.HttpResponseStatus.*
import io.netty.handler.codec.http.HttpUtil
import java.nio.charset.Charset


class HttpServerHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest) {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess) {
            sendHttpResponse(
                ctx, req, DefaultFullHttpResponse(
                    req.protocolVersion(), BAD_REQUEST,
                    ctx.alloc().buffer(0)
                )
            )
            return
        }

        // Allow only GET methods.

        // Allow only GET methods.
        if (!GET.equals(req.method())) {
            sendHttpResponse(
                ctx, req, DefaultFullHttpResponse(
                    req.protocolVersion(), FORBIDDEN,
                    ctx.alloc().buffer(0)
                )
            )
            return
        }

        // Return index page
        if ("/" == req.uri() || "/index.html" == req.uri()) {
            val content: ByteBuf = createContent()
            val res: FullHttpResponse = DefaultFullHttpResponse(req.protocolVersion(), OK, content)
            res.headers()[CONTENT_TYPE] = "text/html; charset=UTF-8"
            HttpUtil.setContentLength(res, content.readableBytes().toLong())
            sendHttpResponse(ctx, req, res)
        } else {
            sendHttpResponse(
                ctx, req, DefaultFullHttpResponse(
                    req.protocolVersion(), NOT_FOUND,
                    ctx.alloc().buffer(0)
                )
            )
        }
    }

    private fun createContent(): ByteBuf {
        return Unpooled.copiedBuffer(
            "<h1>Hello, World!</h1>Connect with websocket to <pre>/ws</pre>", Charset.defaultCharset()
        )
    }

    private fun sendHttpResponse(ctx: ChannelHandlerContext, req: FullHttpRequest, res: FullHttpResponse) {
        // Generate an error page if response getStatus code is not OK (200).
        val responseStatus = res.status()
        if (responseStatus.code() != 200) {
            ByteBufUtil.writeUtf8(res.content(), responseStatus.toString())
            HttpUtil.setContentLength(res, res.content().readableBytes().toLong())
        }
        // Send the response and close the connection if necessary.
        val keepAlive = HttpUtil.isKeepAlive(req) && responseStatus.code() == 200
        HttpUtil.setKeepAlive(res, keepAlive)
        val future = ctx.writeAndFlush(res)
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE)
        }
    }
}