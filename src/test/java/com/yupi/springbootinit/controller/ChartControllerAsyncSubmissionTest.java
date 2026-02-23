package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.bizmq.BiMessageProducer;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartControllerAsyncSubmissionTest {

    private ChartController chartController;

    @Mock
    private ChartService chartService;

    @Mock
    private UserService userService;

    @Mock
    private RedisLimiterManager redisLimiterManager;

    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    @Mock
    private BlockingQueue<Runnable> executorQueue;

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
        ReflectionTestUtils.setField(chartController, "threadPoolExecutor", threadPoolExecutor);
        ReflectionTestUtils.setField(chartController, "biMessageProducer", biMessageProducer);
    }

    @Test
    void genChartByAiAsyncShouldRejectBeforeCreateTaskWhenExecutorSaturated() {
        User loginUser = buildUser(101L, 5);
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(threadPoolExecutor.getActiveCount()).thenReturn(8);
        when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(8);
        when(threadPoolExecutor.getQueue()).thenReturn(executorQueue);
        when(executorQueue.remainingCapacity()).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.genChartByAiAsync(buildCsvFile(), buildRequest(), request));

        assertEquals(ErrorCode.CHART_TASK_REJECTED.getCode(), exception.getCode());
        verify(redisLimiterManager).doRateLimit("genChartByAi_101");
        verify(chartService, never()).createAsyncThreadTask(any(), any());
        verify(chartService, never()).buildUserInput(anyString(), anyString(), anyString());
    }

    @Test
    void genChartByAiAsyncShouldMarkTaskFailedWhenSubmitToExecutorRejected() {
        User loginUser = buildUser(202L, 3);
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(threadPoolExecutor.getActiveCount()).thenReturn(1);
        when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(8);

        when(chartService.buildUserInput(anyString(), anyString(), anyString())).thenReturn("user-input");
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(99L);
        when(chartService.createAsyncThreadTask(any(), eq(loginUser))).thenReturn(biResponse);

        when(threadPoolExecutor.getQueue()).thenReturn(executorQueue);
        when(executorQueue.remainingCapacity()).thenReturn(8);
        when(threadPoolExecutor.execute(any(Runnable.class))).thenThrow(new RejectedExecutionException("busy"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.genChartByAiAsync(buildCsvFile(), buildRequest(), request));

        assertEquals(ErrorCode.CHART_TASK_REJECTED.getCode(), exception.getCode());
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(chartService).handleChartUpdateError(eq(99L), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains(String.valueOf(ErrorCode.CHART_TASK_REJECTED.getCode())));
    }

    private User buildUser(long userId, int points) {
        User user = new User();
        user.setId(userId);
        user.setPoints(points);
        return user;
    }

    private GenChartByAiRequest buildRequest() {
        GenChartByAiRequest request = new GenChartByAiRequest();
        request.setName("test");
        request.setGoal("分析销量趋势");
        request.setChartType("line");
        return request;
    }

    private MockMultipartFile buildCsvFile() {
        return new MockMultipartFile(
                "file",
                "demo.csv",
                "text/csv",
                "date,sales\n2026-01,10\n".getBytes());
    }
}
