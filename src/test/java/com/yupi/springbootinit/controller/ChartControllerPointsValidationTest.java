package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartControllerPointsValidationTest {

    private ChartController chartController;

    @Mock
    private RedisLimiterManager redisLimiterManager;

    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    @Mock
    private BlockingQueue<Runnable> executorQueue;

    @BeforeEach
    void setUp() {
        chartController = new ChartController();
        ReflectionTestUtils.setField(chartController, "redisLimiterManager", redisLimiterManager);
        ReflectionTestUtils.setField(chartController, "threadPoolExecutor", threadPoolExecutor);
    }

    @Test
    void checkPointsAndRateLimitShouldRejectNullUser() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(chartController, "checkPointsAndRateLimit", (User) null));

        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), exception.getCode());
        verifyNoInteractions(redisLimiterManager);
    }

    @Test
    void checkPointsAndRateLimitShouldRejectNullPoints() {
        User user = new User();
        user.setId(1L);
        user.setPoints(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(chartController, "checkPointsAndRateLimit", user));

        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getCode());
        verifyNoInteractions(redisLimiterManager);
    }

    @Test
    void checkPointsAndRateLimitShouldRejectZeroPoints() {
        User user = new User();
        user.setId(1L);
        user.setPoints(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(chartController, "checkPointsAndRateLimit", user));

        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getCode());
        verifyNoInteractions(redisLimiterManager);
    }

    @Test
    void checkPointsAndRateLimitShouldPassAndTriggerRateLimitWhenPointsEnough() {
        User user = new User();
        user.setId(123L);
        user.setPoints(2);

        ReflectionTestUtils.invokeMethod(chartController, "checkPointsAndRateLimit", user);

        verify(redisLimiterManager).doRateLimit("genChartByAi_123");
    }

    @Test
    void isAsyncExecutorSaturatedShouldReturnTrueWhenPoolAndQueueAreFull() {
        when(threadPoolExecutor.getActiveCount()).thenReturn(8);
        when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(8);
        when(threadPoolExecutor.getQueue()).thenReturn(executorQueue);
        when(executorQueue.remainingCapacity()).thenReturn(0);

        Boolean saturated = ReflectionTestUtils.invokeMethod(chartController, "isAsyncExecutorSaturated");

        assertTrue(Boolean.TRUE.equals(saturated));
    }

    @Test
    void isAsyncExecutorSaturatedShouldReturnFalseWhenQueueHasRemainingCapacity() {
        when(threadPoolExecutor.getActiveCount()).thenReturn(8);
        when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(8);
        when(threadPoolExecutor.getQueue()).thenReturn(executorQueue);
        when(executorQueue.remainingCapacity()).thenReturn(1);

        Boolean saturated = ReflectionTestUtils.invokeMethod(chartController, "isAsyncExecutorSaturated");

        assertFalse(Boolean.TRUE.equals(saturated));
    }
}
