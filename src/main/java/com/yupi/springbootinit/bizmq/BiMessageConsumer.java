package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * BI 消息消费者
 * 消费 RabbitMQ 消息，调用 AI 生成图表
 */
@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    // 指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        if (StringUtils.isBlank(message)) {
            rejectMessage(channel, deliveryTag, "empty_message", message);
            return;
        }

        Long chartId = parseChartId(message);
        if (chartId == null) {
            rejectMessage(channel, deliveryTag, "invalid_chart_id", message);
            return;
        }

        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            rejectMessage(channel, deliveryTag, "chart_not_found", message);
            return;
        }

        String userInput = chartService.buildUserInput(chart.getGoal(), chart.getChartType(), chart.getChartData());

        boolean executeSuccess;
        try {
            executeSuccess = chartService.executeChartGeneration(chartId, userInput);
        } catch (Exception e) {
            log.error("BI 图表任务执行异常 chartId={}, message={}", chartId, message, e);
            rejectMessage(channel, deliveryTag, "execute_exception", message);
            return;
        }

        if (!executeSuccess) {
            rejectMessage(channel, deliveryTag, "execute_failed", message);
            return;
        }

        // 消息确认
        channel.basicAck(deliveryTag, false);
    }

    private Long parseChartId(String message) {
        try {
            return Long.parseLong(StringUtils.trim(message));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SneakyThrows
    private void rejectMessage(Channel channel, long deliveryTag, String reason, String message) {
        log.warn("拒绝 BI 消息 reason={}, deliveryTag={}, message={}", reason, deliveryTag, message);
        channel.basicNack(deliveryTag, false, false);
    }
}
