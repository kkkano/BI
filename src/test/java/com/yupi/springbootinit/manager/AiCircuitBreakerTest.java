package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.config.AiCircuitBreakerProperties;
import com.yupi.springbootinit.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiCircuitBreakerTest {

    @Test
    void shouldOpenAfterFailureThresholdReached() {
        AiCircuitBreaker breaker = buildBreaker(true, 2, 2, 1);

        assertThrows(BusinessException.class,
                () -> breaker.execute(() -> {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "mock failure 1");
                }));
        assertThrows(BusinessException.class,
                () -> breaker.execute(() -> {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "mock failure 2");
                }));

        BusinessException openException = assertThrows(BusinessException.class,
                () -> breaker.execute(() -> "ok"));
        assertEquals(ErrorCode.AI_CIRCUIT_BREAKER_OPEN.getCode(), openException.getCode());
    }

    @Test
    void shouldHalfOpenAndRecoverAfterOpenWindow() throws InterruptedException {
        AiCircuitBreaker breaker = buildBreaker(true, 1, 1, 1);

        assertThrows(BusinessException.class,
                () -> breaker.execute(() -> {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "mock failure");
                }));

        BusinessException openException = assertThrows(BusinessException.class,
                () -> breaker.execute(() -> "ok"));
        assertEquals(ErrorCode.AI_CIRCUIT_BREAKER_OPEN.getCode(), openException.getCode());

        Thread.sleep(1100L);

        assertEquals("probe-ok", breaker.execute(() -> "probe-ok"));
        assertEquals("closed-ok", breaker.execute(() -> "closed-ok"));
    }

    @Test
    void shouldReopenWhenHalfOpenProbeFails() throws InterruptedException {
        AiCircuitBreaker breaker = buildBreaker(true, 1, 1, 1);

        assertThrows(BusinessException.class,
                () -> breaker.execute(() -> {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "mock failure");
                }));

        Thread.sleep(1100L);

        assertThrows(BusinessException.class,
                () -> breaker.execute(() -> {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "probe failed");
                }));

        BusinessException openException = assertThrows(BusinessException.class,
                () -> breaker.execute(() -> "ok"));
        assertEquals(ErrorCode.AI_CIRCUIT_BREAKER_OPEN.getCode(), openException.getCode());
    }

    private AiCircuitBreaker buildBreaker(boolean enabled,
                                          int failureThreshold,
                                          int openDurationSeconds,
                                          int halfOpenSuccessThreshold) {
        AiCircuitBreakerProperties properties = new AiCircuitBreakerProperties();
        properties.setEnabled(enabled);
        properties.setFailureThreshold(failureThreshold);
        properties.setOpenDurationSeconds(openDurationSeconds);
        properties.setHalfOpenSuccessThreshold(halfOpenSuccessThreshold);

        AiCircuitBreaker breaker = new AiCircuitBreaker();
        ReflectionTestUtils.setField(breaker, "properties", properties);
        return breaker;
    }
}
