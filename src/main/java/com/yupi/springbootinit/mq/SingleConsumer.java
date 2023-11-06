package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;

public class SingleConsumer {
	// 定义我们正在监听的队列名称
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {
         // 创建连接,创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("test");
        factory.setPassword("test");
        // 设置连接工厂的主机名，这里我们连接的是本地的RabbitMQ服务器
        factory.setHost("localhost");
    	// 从工厂获取一个新的连接
        Connection connection = factory.newConnection();
        // 从连接中创建一个新的频道
        Channel channel = connection.createChannel();
    	// 创建队列,在该频道上声明我们正在监听的队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 在控制台打印等待接收消息的信息
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
    	// 定义了如何处理消息,创建一个新的DeliverCallback来处理接收到的消息
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            // 将消息体转换为字符串
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            // 在控制台打印已接收消息的信息
            System.out.println(" [x] Received '" + message + "'");
        };
        // 在频道上开始消费队列中的消息，接收到的消息会传递给deliverCallback来处理,会持续阻塞
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }
}