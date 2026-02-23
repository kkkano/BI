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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChartControllerPointsValidationTest {

    private ChartController chartController;

    @Mock
    private RedisLimiterManager redisLimiterManager;

    @BeforeEach
    void setUp() {
        chartController = new ChartController();
        ReflectionTestUtils.setField(chartController, "redisLimiterManager", redisLimiterManager);
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
}
