package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.config.AiCircuitBreakerProperties;
import com.yupi.springbootinit.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.Callable;

/**
 * AI 调用熔断器（CLOSED -> OPEN -> HALF_OPEN）。
 */
@Slf4j
@Component
public class AiCircuitBreaker {

    private enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private static final int MIN_THRESHOLD = 1;

    @Resource
    private AiCircuitBreakerProperties properties;

    private final Object stateLock = new Object();

    private CircuitState state = CircuitState.CLOSED;

    private int consecutiveFailureCount = 0;

    private int halfOpenSuccessCount = 0;

    private boolean halfOpenCallInFlight = false;

    private long openUntilEpochMillis = 0L;

    /**
     * 在熔断器保护下执行 AI 调用。
     */
    public <T> T execute(Callable<T> action) {
        if (action == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI 调用任务不能为空");
        }
        if (!properties.isEnabled()) {
            return callAction(action);
        }

        if (!tryAcquirePermission()) {
            throw new BusinessException(ErrorCode.AI_CIRCUIT_BREAKER_OPEN, buildOpenMessage());
        }

        try {
            T result = callAction(action);
            onSuccess();
            return result;
        } catch (BusinessException e) {
            onFailure(e);
            throw e;
        } catch (RuntimeException e) {
            onFailure(e);
            throw e;
        }
    }

    private <T> T callAction(Callable<T> action) {
        try {
            return action.call();
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 调用异常: " + e.getMessage());
        }
    }

    private boolean tryAcquirePermission() {
        synchronized (stateLock) {
            long now = System.currentTimeMillis();
            if (state == CircuitState.OPEN) {
                if (now < openUntilEpochMillis) {
                    return false;
                }
                state = CircuitState.HALF_OPEN;
                halfOpenCallInFlight = true;
                halfOpenSuccessCount = 0;
                log.info("AI 熔断器进入 HALF_OPEN，允许探测请求");
                return true;
            }

            if (state == CircuitState.HALF_OPEN) {
                if (halfOpenCallInFlight) {
                    return false;
                }
                halfOpenCallInFlight = true;
                return true;
            }

            return true;
        }
    }

    private void onSuccess() {
        synchronized (stateLock) {
            if (state == CircuitState.HALF_OPEN) {
                halfOpenCallInFlight = false;
                halfOpenSuccessCount++;
                int successThreshold = normalizedThreshold(properties.getHalfOpenSuccessThreshold());
                if (halfOpenSuccessCount >= successThreshold) {
                    transitionToClosed();
                }
                return;
            }
            if (state == CircuitState.CLOSED && consecutiveFailureCount > 0) {
                consecutiveFailureCount = 0;
            }
        }
    }

    private void onFailure(Throwable throwable) {
        synchronized (stateLock) {
            if (state == CircuitState.HALF_OPEN) {
                halfOpenCallInFlight = false;
                transitionToOpen("half_open_probe_failed", throwable);
                return;
            }
            if (state == CircuitState.CLOSED) {
                consecutiveFailureCount++;
                int failureThreshold = normalizedThreshold(properties.getFailureThreshold());
                if (consecutiveFailureCount >= failureThreshold) {
                    transitionToOpen("closed_failure_threshold_reached", throwable);
                }
            }
        }
    }

    private void transitionToClosed() {
        state = CircuitState.CLOSED;
        consecutiveFailureCount = 0;
        halfOpenSuccessCount = 0;
        halfOpenCallInFlight = false;
        openUntilEpochMillis = 0L;
        log.info("AI 熔断器恢复到 CLOSED");
    }

    private void transitionToOpen(String reason, Throwable throwable) {
        state = CircuitState.OPEN;
        consecutiveFailureCount = 0;
        halfOpenSuccessCount = 0;
        halfOpenCallInFlight = false;
        openUntilEpochMillis = System.currentTimeMillis() + normalizedOpenDurationMillis();
        log.warn("AI 熔断器打开 reason={}, openDurationSeconds={}, error={}",
                reason,
                normalizedOpenDurationMillis() / 1000,
                throwable == null ? "unknown" : throwable.getMessage());
    }

    private String buildOpenMessage() {
        long remainingMillis;
        synchronized (stateLock) {
            remainingMillis = Math.max(0L, openUntilEpochMillis - System.currentTimeMillis());
        }
        long remainingSeconds = (remainingMillis + 999L) / 1000L;
        if (remainingSeconds <= 0) {
            return ErrorCode.AI_CIRCUIT_BREAKER_OPEN.getMessage();
        }
        return String.format("AI 服务暂时不可用，请 %d 秒后重试", remainingSeconds);
    }

    private int normalizedThreshold(int threshold) {
        return Math.max(MIN_THRESHOLD, threshold);
    }

    private long normalizedOpenDurationMillis() {
        return 1000L * Math.max(MIN_THRESHOLD, properties.getOpenDurationSeconds());
    }
}
