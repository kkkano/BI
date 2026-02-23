package com.yupi.springbootinit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 熔断器配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.circuit-breaker")
public class AiCircuitBreakerProperties {

    /**
     * 是否启用熔断器。
     */
    private boolean enabled = true;

    /**
     * 连续失败阈值，达到后进入 OPEN。
     */
    private int failureThreshold = 3;

    /**
     * OPEN 状态保持时长（秒）。
     */
    private int openDurationSeconds = 30;

    /**
     * HALF_OPEN 阶段恢复到 CLOSED 需要的连续成功次数。
     */
    private int halfOpenSuccessThreshold = 1;
}
