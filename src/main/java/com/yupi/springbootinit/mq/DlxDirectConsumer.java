package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxDirectConsumer {
	// 定义我们正在监听的死信交换机名称"dlx-direct-exchange"
    private static final String DEAD_EXCHANGE_NAME = "dlx-direct-exchange";
	// 定义我们正在监听的业务交换机名称"direct2-exchange"
    private static final String WORK_EXCHANGE_NAME = "direct2-exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("test");
        factory.setPassword("test");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(WORK_EXCHANGE_NAME, "direct");

        // 创建用于指定死信队列的参数的Map对象
        Map<String, Object> args = new HashMap<>();
        // 将要创建的队列绑定到指定的交换机，并设置死信队列的参数
        args.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        // 指定死信要转发到外包死信队列
        args.put("x-dead-letter-routing-key", "waibao");

        // 创建新的小狗队列，并将其绑定到业务交换机，使用"xiaodog"作为路由键
        String queueName = "xiaodog_queue";
        channel.queueDeclare(queueName, true, false, false, args);
        channel.queueBind(queueName, WORK_EXCHANGE_NAME, "xiaodog");

        Map<String, Object> args2 = new HashMap<>();
        args2.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        // 指定死信要转发到老板死信队列
        args2.put("x-dead-letter-routing-key", "laoban");

        // 创建新的小猫队列，并将其绑定到业务交换机，使用"xiaocat"作为路由键
        String queueName2 = "xiaocat_queue";
        channel.queueDeclare(queueName2, true, false, false, args2);
        channel.queueBind(queueName2, WORK_EXCHANGE_NAME, "xiaocat");
    	// 打印等待消息的提示信息
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
    	// 创建用于处理小狗队列消息的回调函数，当接收到消息时，拒绝消息并打印消息内容
        DeliverCallback xiaoyuDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            // 拒绝消息，并且不要重新将消息放回队列，只拒绝当前消息
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [xiaodog] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
    	// 创建用于处理小猫队列消息的回调函数，当接收到消息时，拒绝消息并打印消息内容
        DeliverCallback xiaopiDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
        	// 拒绝消息，并且不要重新将消息放回队列，只拒绝当前消息
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [xiaocat] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
    	// 注册消费者，用于消费小狗队列，绑定回调函数，自动确认消息改为false
        channel.basicConsume(queueName, false, xiaoyuDeliverCallback, consumerTag -> {
        });
        // 注册消费者，用于消费小猫队列，绑定回调函数
        channel.basicConsume(queueName2, false, xiaopiDeliverCallback, consumerTag -> {
        });
    }
}