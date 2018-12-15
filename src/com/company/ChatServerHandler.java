package com.company;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ChatServerHandler extends ChannelInboundHandlerAdapter
{

    HashMap<String, ChannelHandlerContext> channelAndID = new HashMap();
    private String userId;
    private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception
    {
        System.out.println("handlerAdded of [SERVER]");
        Channel incoming = ctx.channel();

        for (Channel channel : channelGroup)
        {
            //사용자가 추가되었을 때 기존 사용자에게 알림
            channel.write("[SERVER] - " + incoming.remoteAddress() + "has joined!\n");
        }
        channelGroup.add(incoming);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception
    {
        super.channelRegistered(ctx);
        System.out.println("채널 등록 됨");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        // 사용자가 접속했을 때 서버에 표시.
       System.out.println("User Access! "+ctx.channel().remoteAddress());
       System.out.println("User Access! "+ ctx.channel().id().asLongText());

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
    {
        System.out.println("handlerRemoved of [SERVER]");
        Channel incoming = ctx.channel();
        for (Channel channel : channelGroup)
        {
            //사용자가 나갔을 때 기존 사용자에게 알림
            //channel.write("[SERVER] - " + incoming.remoteAddress() + "has left!\n");
        }
        channelGroup.remove(incoming);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
    {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        String message = null;
        message = (String) msg;


//        요약
//        'message: '가 없이 온 내용은 클라의 아이디이고 있으면 채팅 내용임
//        quiz: 로 시작하는 값은 관리자의 퀴즈

//        클라쪽에서 접속을 하자마자 아이디를 보냄
//        하지만 서버 쪽에서 도착한 내용이 아이디인지 채팅 내용인지 구분 해야 함
//        그래서 클라쪽에서 채팅을 보낼때는 내용 앞에 'message: ' 를 붙임


        System.out.println("channelRead of [SERVER]: " + message);

        if(!message.startsWith("message"))
        {
            channelAndID.put(message, ctx);
            System.out.println("입장시 피아식별: "+message);
        }

        else
        {
            Channel incoming = ctx.channel();
            for (Channel channel : channelGroup)
            {
                if (channel != incoming) //자기 한테 또 보내지 않기 위해
                {
                    //메시지 전달.
                    //channel.writeAndFlush("[" + incoming.remoteAddress() + "/" + incoming.id().toString() + "]" + message + "\n");
                    channel.writeAndFlush(message.substring(8) + "\n");
                }
            }
            if ("bye".equals(message.toLowerCase()))
            {
                ctx.close();
            }

        }

    }

}
