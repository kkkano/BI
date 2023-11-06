package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;

public class FanoutProducer {
    // 定义要使用的交换机名名称,这次的队列就改叫fanout-exchange
    private static final String EXCHANGE_NAME = "fanout-exchange";

    public static void main(String[] argv) throws Exception {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        // 设置连接工厂的主机地址
        factory.setHost("localhost");
        factory.setUsername("test");
        factory.setPassword("test");
        // 创建连接和通道
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 声明fanout类型的交换机
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            // 创建一个Scanner对象来读取用户输入
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                // 从用户输入中获取消息内容
                String message = scanner.nextLine();
                // 将消息发送到指定的交换机（fanout交换机），不指定路由键（空字符串）
                channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes("UTF-8"));
                // 打印发送的消息内容
                System.out.println(" [x] Sent '" + message + "'");
            }
        }
    }
}