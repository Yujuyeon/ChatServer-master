package com.company;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.mysql.cj.log.Log;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

public class quizServerHandler extends ChannelInboundHandlerAdapter
{

    HashMap<ChannelHandlerContext, String> channelAndID = new HashMap();
    HashMap<String, Boolean> idAndBoolean = new HashMap();

    private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/db?characterEncoding=UTF-8&serverTimezone=UTC";

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception
    {
        System.out.println("handlerAdded of [SERVER]");
        Channel incoming = ctx.channel();

        for (Channel channel : channelGroup)
        {
            //사용자가 추가되었을 때 기존 사용자에게 알림
//            channel.write("[SERVER] - " + incoming.remoteAddress() + "입장합니다.\n");
        }
        channelGroup.add(incoming);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception
    {
        super.channelRegistered(ctx);
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


        System.out.println("channelRead of [SERVER]: " + message);

        if(message.startsWith("id:"))
        {
            message = message.replace("id:","");
            channelAndID.put(ctx, message);
            idAndBoolean.put(message, true); //정답자와 오답자를 구별하기 위함
            System.out.println("입장시 아이디 해시에 저장: "+message);
        }

        else
        {
            Channel incoming = ctx.channel();

//            if(channelAndID.get(ctx).equals("admin"))
            if(message.startsWith("admin"))
            {
                System.out.println("관리자로부터 메시지");
                System.out.println(1);
                for (Channel channel : channelGroup)
                {
                    if (channel != incoming) //자기 한테 또 보내지 않기 위해
                    {
                        //메시지 전달.
                        System.out.println("퀴즈 전달");
                        //channel.writeAndFlush("[" + incoming.remoteAddress() + "/" + incoming.id().toString() + "]" + message + "\n");
                        channel.writeAndFlush(message.substring(6));
                    }
                }
            }
            else if (message.startsWith("userAnswer"))
            {
                System.out.println("시청자로부터 온 퀴즈 정답: " + message);
//                '문제 번호 | 사용자 아이디 | 풀이' 형태의 데이터를 받음
                String[] getAnswer = message.split("\\|");
                System.out.println("시청자의 답:"+Integer.parseInt(getAnswer[3]));
                System.out.println(getAnswerFromDB(Integer.parseInt(getAnswer[1].substring(1,2))));
                if (Integer.parseInt(getAnswer[3]) == getAnswerFromDB(Integer.parseInt(getAnswer[1].substring(1,2))))
                {
                    System.out.println(getAnswer[2] + " 정답");
                    idAndBoolean.put(channelAndID.get(ctx), true);
                }
                else
                {
                    System.out.println(getAnswer[2] + " 오");
                    idAndBoolean.put(channelAndID.get(ctx), false);
                }
            }

            if ("bye".equals(message.toLowerCase()))
            {
                ctx.close();
            }
        }

    }

    private int getAnswerFromDB(int quizNumber)
    {
        int answer = 0;
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try
        {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, "johnny", "sql");
            stmt = conn.createStatement();
            sql = "select answer from quiz where quizNumber = " + quizNumber;
            ResultSet rs = stmt.executeQuery(sql);
            rs.next();
            answer = rs.getInt("answer");
            System.out.println("db정답: "+rs.getInt("answer"));
            rs.close();
            stmt.close();
            conn.close();
        }
        catch (Exception e)
        {
            System.out.println("nnnn");
            e.printStackTrace();
        }

        return answer;

    }

}
