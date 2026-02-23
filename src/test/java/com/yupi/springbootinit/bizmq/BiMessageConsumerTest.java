package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BiMessageConsumerTest {

    private final BiMessageConsumer consumer = new BiMessageConsumer();

    private ChartService chartService;

    private Channel channel;

    @BeforeEach
    void setUp() {
        chartService = mock(ChartService.class);
        channel = mock(Channel.class);
        ReflectionTestUtils.setField(consumer, "chartService", chartService);
    }

    @Test
    void shouldRejectWhenMessageIsBlank() {
        consumer.receiveMessage("  ", channel, 1L);

        verify(channel).basicNack(1L, false, false);
        verifyNoInteractions(chartService);
    }

    @Test
    void shouldRejectWhenMessageIsNotNumericId() {
        consumer.receiveMessage("not-a-number", channel, 2L);

        verify(channel).basicNack(2L, false, false);
        verifyNoInteractions(chartService);
    }

    @Test
    void shouldAckWhenChartTaskExecutedSuccessfully() {
        Chart chart = new Chart();
        chart.setGoal("分析销售额");
        chart.setChartType("折线图");
        chart.setChartData("date,value");
        when(chartService.getById(3L)).thenReturn(chart);
        when(chartService.buildUserInput("分析销售额", "折线图", "date,value")).thenReturn("userInput");
        when(chartService.executeChartGeneration(3L, "userInput")).thenReturn(true);

        consumer.receiveMessage("3", channel, 3L);

        verify(channel).basicAck(3L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void shouldRejectWhenChartTaskExecutionFailed() {
        Chart chart = new Chart();
        chart.setGoal("分析销售额");
        chart.setChartType("折线图");
        chart.setChartData("date,value");
        when(chartService.getById(4L)).thenReturn(chart);
        when(chartService.buildUserInput("分析销售额", "折线图", "date,value")).thenReturn("userInput");
        when(chartService.executeChartGeneration(4L, "userInput")).thenReturn(false);

        consumer.receiveMessage("4", channel, 4L);

        verify(channel).basicNack(4L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void shouldRejectWhenChartTaskExecutionThrowsException() {
        Chart chart = new Chart();
        chart.setGoal("分析销售额");
        chart.setChartType("折线图");
        chart.setChartData("date,value");
        when(chartService.getById(5L)).thenReturn(chart);
        when(chartService.buildUserInput("分析销售额", "折线图", "date,value")).thenReturn("userInput");
        when(chartService.executeChartGeneration(5L, "userInput")).thenThrow(new RuntimeException("boom"));

        consumer.receiveMessage("5", channel, 5L);

        verify(channel).basicNack(5L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(chartService).executeChartGeneration(5L, "userInput");
    }

    @Test
    void shouldRejectWhenChartNotFound() {
        when(chartService.getById(6L)).thenReturn(null);

        consumer.receiveMessage("6", channel, 6L);

        verify(channel).basicNack(6L, false, false);
        verify(chartService, never()).buildUserInput(anyString(), anyString(), anyString());
        verify(chartService, never()).executeChartGeneration(anyLong(), anyString());
    }
}
