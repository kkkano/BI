package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.util.Scanner;

public class MultiProducer {
    // 定义要使用的队列名称,这次的队列就改叫multi_queue
    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {
        // 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        // 设置RabbitMQ服务的主机名
        factory.setHost("localhost");
        factory.setUsername("test");
        factory.setPassword("test");
        // 创建一个新的连接
        try (Connection connection = factory.newConnection();
             // 创建一个新的频道
             Channel channel = connection.createChannel()) {
            // 声明队列参数，包括队列名称、是否持久化等
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            // 创建一个输入扫描器，用于读取控制台输入
            Scanner scanner = new Scanner(System.in);
            // 使用循环，每当用户在控制台输入一行文本，就将其作为消息发送
            while (scanner.hasNext()) {
                // 读取用户在控制台输入的下一行文本
                String message = scanner.nextLine();
                // 发布消息到队列，设置消息持久化
                channel.basicPublish("", TASK_QUEUE_NAME,
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes("UTF-8"));
                // 输出到控制台，表示消息已发送
                System.out.println(" [x] Sent '" + message + "'");
            }
        }
    }
}