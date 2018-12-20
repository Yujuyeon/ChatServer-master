package com.company;

import java.sql.*;
import java.util.ArrayList;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ServerChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class quizServerHandler extends ChannelInboundHandlerAdapter
{
    private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private static final int GET_QUIZ = 1000;
    private int quizId, answer, userAnswer;
    private String question, examples;

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/lara?characterEncoding=UTF-8&serverTimezone=UTC";

    private boolean winnerOrNot;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception
    {
        Channel incoming = ctx.channel();
        if(!channelGroup.contains(incoming))
        {
            channelGroup.add(incoming);
        }
        winnerOrNot = true;
        System.out.println("채널 그룹 크기: "+channelGroup.size());

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
    {
        System.out.println("핸들러 제거");
        Channel incoming = ctx.channel();
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

        System.out.println("받은 메시지: " + message);
        Channel incoming = ctx.channel();

//        메시지 별 해설
//            1. quiz/1(뒤에 숫자는 퀴즈 번호를 뜻함)
//            - 호스트가 퀴즈내기 버튼을 누르면 quiz1 메시지를 보냄
//            - 서버는 db에서 해당 번호의 퀴즈를 가져옴
//            - 퀴즈 내용을 호스트를 제외한 채널 그룹에 있는 모든 사용자에게 전달
//
//            2. userAnswer/1/1(뒤에 숫자는 문항번호/유저의 답 순서임)
//            - 유저가 퀴즈를 풀어서 정답을 제출함
//            - 유저의 답이 정답인지 아닌지 확인
//            - 정답인지 오답인지 전송
//
//            3. score
//            - 호스트가 채점하기 버튼을 누름
//            - 모든 유저에게 score라는 메시지를 모냄


        if(message.split("/")[0].equals("quiz"))
        {
//            퀴즈는 번호|문제|보기1/2/3 형태로 전달 받음
            System.out.println("quiz:"+getDataFromDB(GET_QUIZ, Integer.parseInt(message.split("/")[1])));
//                System.out.println("관리자로부터 메시지");
           for (Channel channel : channelGroup)
            {
                if (channel != incoming) //자기 한테 또 보내지 않기 위해
                {
                    //메시지 전달.
//                        System.out.println("퀴즈 전달");
                    //channel.writeAndFlush("[" + incoming.remoteAddress() + "/" + incoming.id().toString() + "]" + message + "\n");
                    channel.writeAndFlush("goQuiz"+getDataFromDB(GET_QUIZ, Integer.parseInt(message.split("/")[1])));
                }
            }
        }
        else if (message.startsWith("userAnswer"))
        {
            String[] getAnswer = message.split("/");
            userAnswer = Integer.parseInt(getAnswer[2]);
            answer = Integer.parseInt(getDataFromDB(2, Integer.parseInt(message.split("/")[1])));
//            answer = Integer.parseInt(getDataFromDB(2, Integer.parseInt(message.split("/")[1])));
//            System.out.println("db정답: " + getDataFromDB(2, Integer.parseInt(message.split("/")[1])));

            for (Channel channel : channelGroup)
            {
                if (channel == incoming) //자기 한테 또 보내지 않기 위해
                {
                    if (userAnswer == answer)
                    {
                        System.out.println(getAnswer[2] + " 정답");
                        channel.writeAndFlush("o");

                        if(winnerOrNot)
                        {
                            winnerOrNot = true;
                        }
//                            idAndBoolean.put(getAnswer[2], true);
                    }
                    else
                    {
                        System.out.println(getAnswer[2] + " 오답");
                        channel.writeAndFlush("x");
                        winnerOrNot = false;
//                            idAndBoolean.put(getAnswer[2], false);
                    }
                }
            }
        }

        else if(message.equals("score"))
        {
            for (Channel channel : channelGroup)
            {
                if (channel != incoming)
                {
                    //메시지 전달.
                    channel.writeAndFlush("score");
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

        else if(message.equals("winnerOrNot"))
        {
            if(winnerOrNot)
            {
                ctx.writeAndFlush("최종 우승자!");
            }
        }

//       테스 보냄 사람한테만 보냄
        else if(message.startsWith("test"))
        {
            System.out.println("in");

            for (Channel channel : channelGroup)
            {
                if (channel == incoming) // 자가 테스트
                {
                    channel.writeAndFlush("test done from server");
                }
            }
        }

        if ("bye".equals(message.toLowerCase()))
        {
            ctx.close();
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
    {
        super.channelUnregistered(ctx);
    }

    private String getDataFromDB(int purpose, int quizNumber)
    {
        Connection conn = null;
        Statement stmt = null;
        String sql = null;
        String dataFromQuizDB = null;

        try
        {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, "johnny", "sql");
            stmt = conn.createStatement();

            if(purpose == GET_QUIZ)
            {
                sql = "select * from quiz where id = " + quizNumber;
            }
            else
            {
                sql = "select answer from quiz where id = " + quizNumber;
            }
            ResultSet rs = stmt.executeQuery(sql);

            if(purpose == GET_QUIZ)
            {
                while(rs.next())
                {
                    quizId = Integer.parseInt(rs.getString("id"));
                    question = rs.getString("question");
                    examples = rs.getString("examples");
                    dataFromQuizDB = quizId+"|"+question+"|"+examples;
//                    System.out.println(id+question+examples);
                }
            }
            else
            {
                rs.next();
                dataFromQuizDB = rs.getString("answer");
            }

            rs.close();
            stmt.close();
            conn.close();
        }
        catch (Exception e)
        {
            System.out.println("nnnn");
            e.printStackTrace();
        }

        return dataFromQuizDB;

    }

}

