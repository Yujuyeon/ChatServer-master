package com.company;


import java.util.ArrayList;
import java.util.HashMap;
import java.sql.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class quizServerHandler extends ChannelInboundHandlerAdapter
{

//    HashMap<String, Channel> idAndChannel = new HashMap();
//    HashMap<String, Boolean> idAndBoolean = new HashMap();
//    ArrayList<String> idList = new ArrayList<>();
    private int userAnswer, dbAnswer;
    int chk = 1;
    private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/lara?characterEncoding=UTF-8&serverTimezone=UTC";

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
        System.out.println(channelGroup.size());
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
//       System.out.println("User Access! "+ctx.channel().remoteAddress());
//       System.out.println("User Access! "+ ctx.channel().id().asLongText());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
    {
//        System.out.println("핸들러 제거");
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



//        1. 첫입장시 id: 로 시작하는 값을 받음
//            - id:유저 아이디
//        2. admin으로 시작하는 값은 퀴즈를 내기 위한 메시지임
//        3. userAnswer로 시작하는 값은 유저들의 문제 풀이임
//            - idAndBoolean 해시에 정답자와 오답자를 표기함
//        4. correntOrNot으로 시작하는 값은 유저들에게 채점을 하기 위한 메시지임


        if(message.startsWith("id:"))
        {
            message = message.replace("id:","");

            // 인터넷을 재접속하면 channel 값이 바뀔 수 있으므로
            // 해시맵을 두번 써서 ctx -> id -> bool(퀴즈 정답 유무) 형태로 값을 저장하고 역순으로 값을 불러온다.

//            idAndChannel.put(message, ctx.channel());
//
//            if(!idList.contains(message))
//            {
//                idList.add(message);
//            }
//            if(!idAndBoolean.containsKey(message))
//            {
//                System.out.println(message+": true ");
//                idAndBoolean.put(message, true); //정답자와 오답자를 구별하기 위함
//            }
//            System.out.println("입장시 아이디 해시에 저장: "+message);

//            for(int i = 0; i < idList.size(); i++)
//            {
//                System.out.println("등록된 아이디:" +i + "/" + idList.get(i));
//            }
        }

        else
        {
            Channel incoming = ctx.channel();

//            if(channelAndID.get(ctx).equals("admin"))
            if(message.startsWith("admin"))
            {
//                System.out.println("관리자로부터 메시지");
               for (Channel channel : channelGroup)
                {
                    if (channel != incoming) //자기 한테 또 보내지 않기 위해
                    {
                        //메시지 전달.
//                        System.out.println("퀴즈 전달");
                        //channel.writeAndFlush("[" + incoming.remoteAddress() + "/" + incoming.id().toString() + "]" + message + "\n");
                        channel.writeAndFlush("goQuiz"+message.substring(6));
                    }
                }
            }
            else if (message.startsWith("userAnswer"))
            {
//                'userAnswer | 문제 번호 | 사용자 아이디 | 풀이' 형태의 데이터를 받음
//                System.out.println("치널 크기"+channelGroup.size());
//                System.out.println("시청자로부터 온 퀴즈 정답: " + message);
                String[] getAnswer = message.split("\\|");

                userAnswer = Integer.parseInt(getAnswer[3]);
                dbAnswer = getAnswerFromDB(Integer.parseInt(getAnswer[1].substring(2,3)));
                for (Channel channel : channelGroup)
                {
                    if (channel != incoming) //자기 한테 또 보내지 않기 위해
                    {
                        if (userAnswer == dbAnswer)
                        {
                            System.out.println(getAnswer[2] + " 정답");
                            channel.writeAndFlush("퀴즈채점 정답ㅇㅇㅇ");
//                            idAndBoolean.put(getAnswer[2], true);
                        }
                        else
                        {
                            System.out.println(getAnswer[2] + " 오답");
                            channel.writeAndFlush("퀴즈채점 오답ㄴㄴㄴ");
//                            idAndBoolean.put(getAnswer[2], false);
                        }
                    }
                }
            }

            else if(message.startsWith("score"))
            {
                for (Channel channel : channelGroup)
                {
                    if (channel != incoming)
                    {
                        //메시지 전달.
                        channel.writeAndFlush("퀴즈채점 ㅎㅎㅎ");
//                        if(idAndBoolean.get(channelAndID.get(ctx.channel())))
//                        {
//                            System.out.println(channelAndID.get(ctx.channel()) + "님 정답");
//                            channel.writeAndFlush("정답o");
//                        }
//                        else
//                        {
//                            System.out.println(channelAndID.get(ctx.channel()) + "님 오답");
//                            channel.writeAndFlush("오답x");
//                        }
                    }
                }
            }

            if ("bye".equals(message.toLowerCase()))
            {
                ctx.close();
            }
        }

        System.out.println("channelRead of [SERVER]: " + message);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
    {
        super.channelUnregistered(ctx);
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
            sql = "select answer from quiz where id = " + quizNumber;
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
