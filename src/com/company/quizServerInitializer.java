package com.company;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

import java.util.List;

public class quizServerInitializer extends ChannelInitializer<SocketChannel>
{

    private final SslContext sslCtx;

    public quizServerInitializer(SslContext sslCtx)
    {
        this.sslCtx = sslCtx;
    }

    @Override
    protected void initChannel(SocketChannel arg0) throws Exception
    {
        ChannelPipeline pipeline = arg0.pipeline();

        //pipeline.addLast(sslCtx.newHandler(arg0.alloc())); 보안을 강화.
        //pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast(new ByteToMessageDecoder()
        {
            @Override
            public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
            {
                out.add(in.readBytes(in.readableBytes()));
            }
        });


        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());
        pipeline.addLast(new quizServerHandler());
    }

}
