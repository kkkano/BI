package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.model.dto.chart.GenChartByAiRequest;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartControllerAsyncMqTest {

    private ChartController chartController;

    @Mock
    private ChartService chartService;

    @Mock
    private UserService userService;

    @Mock
    private RedisLimiterManager redisLimiterManager;

    @Mock
    private BiMessageProducer biMessageProducer;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        chartController = new ChartController();
        ReflectionTestUtils.setField(chartController, "chartService", chartService);
        ReflectionTestUtils.setField(chartController, "userService", userService);
        ReflectionTestUtils.setField(chartController, "redisLimiterManager", redisLimiterManager);
        ReflectionTestUtils.setField(chartController, "biMessageProducer", biMessageProducer);
    }

    @Test
    void genChartByAiAsyncMqShouldReturnResponseWhenMessageSent() {
        User loginUser = buildUser(1001L, 3);
        when(userService.getLoginUser(request)).thenReturn(loginUser);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(77L);
        when(chartService.createAsyncMqTask(any(), eq(loginUser))).thenReturn(biResponse);

        BaseResponse<BiResponse> response = chartController.genChartByAiAsyncMq(buildCsvFile(), buildRequest(), request);

        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(77L, response.getData().getChartId());
        verify(redisLimiterManager).doRateLimit("genChartByAi_1001");
        verify(biMessageProducer).sendMessage("77");
        verify(chartService, never()).handleChartUpdateError(any(), any());
    }

    @Test
    void genChartByAiAsyncMqShouldMarkFailedWhenMessageSendThrows() {
        User loginUser = buildUser(1002L, 2);
        when(userService.getLoginUser(request)).thenReturn(loginUser);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(88L);
        when(chartService.createAsyncMqTask(any(), eq(loginUser))).thenReturn(biResponse);
        doThrow(new RuntimeException("mq unavailable")).when(biMessageProducer).sendMessage("88");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.genChartByAiAsyncMq(buildCsvFile(), buildRequest(), request));

        assertEquals(ErrorCode.CHART_TASK_MESSAGE_SEND_FAILED.getCode(), exception.getCode());
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(chartService).handleChartUpdateError(eq(88L), messageCaptor.capture());
        assertEquals(true, messageCaptor.getValue().contains(
                String.valueOf(ErrorCode.CHART_TASK_MESSAGE_SEND_FAILED.getCode())));
    }

    @Test
    void genChartByAiAsyncMqShouldRejectWhenCreateTaskResponseLacksChartId() {
        User loginUser = buildUser(1003L, 2);
        when(userService.getLoginUser(request)).thenReturn(loginUser);

        BiResponse biResponse = new BiResponse();
        when(chartService.createAsyncMqTask(any(), eq(loginUser))).thenReturn(biResponse);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.genChartByAiAsyncMq(buildCsvFile(), buildRequest(), request));

        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getCode());
        verify(biMessageProducer, never()).sendMessage(any());
        verify(chartService, never()).handleChartUpdateError(any(), any());
    }

    private User buildUser(long userId, int points) {
        User user = new User();
        user.setId(userId);
        user.setPoints(points);
        return user;
    }

    private GenChartByAiRequest buildRequest() {
        GenChartByAiRequest request = new GenChartByAiRequest();
        request.setName("demo");
        request.setGoal("analyze trend");
        request.setChartType("line");
        return request;
    }

    private MockMultipartFile buildCsvFile() {
        return new MockMultipartFile(
                "file",
                "demo.csv",
                "text/csv",
                "day,value\n2026-01-01,10\n".getBytes());
    }
}
