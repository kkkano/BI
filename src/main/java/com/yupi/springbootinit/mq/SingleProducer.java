package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
// 定义一个名为SingleProducer的公开类，用于实现消息发送功能
public class SingleProducer {
    // 定义一个静态常量字符串QUEUE_NAME，它的值为"hello"，表示我们要向名为"hello"的队列发送消息
    private final static String QUEUE_NAME = "hello";

    // 定义程序的入口点：一个公开的静态main方法，它抛出Exception异常
    public static void main(String[] argv) throws Exception {
        // 创建一个ConnectionFactory对象，这个对象可以用于创建到RabbitMQ服务器的连接
        ConnectionFactory factory = new ConnectionFactory();
        // 设置ConnectionFactory的主机名为"localhost"，这表示我们将连接到本地运行的RabbitMQ服务器
        factory.setHost("localhost");
        // 如果你改了本地的用户名和密码,你可能要指定userName、userPassword,
        // 如果改了本地的端口，还要改Port。
        // 那我们这里不需要,我们这里就用默认的localhost,默认的用户名和密码,就是guest
         factory.setUsername("test");
        factory.setPassword("test");
        // factory.setPort();
        // 使用ConnectionFactory创建一个新的连接,这个连接用于和RabbitMQ服务器进行交互
        try (Connection connection = factory.newConnection();
             // 通过已建立的连接创建一个新的频道
             Channel channel = connection.createChannel()) {
            // 在通道上声明一个队列，我们在此指定的队列名为"hello"
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            // 创建要发送的消息，这里我们将要发送的消息内容设置为"Hello World!"
            String message = "Hello World!";
            // 使用channel.basicPublish方法将消息发布到指定的队列中。这里我们指定的队列名为"hello"
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            // 使用channel.basicPublish方法将消息发布到指定的队列中。这里我们指定的队列名为"hello"
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
}