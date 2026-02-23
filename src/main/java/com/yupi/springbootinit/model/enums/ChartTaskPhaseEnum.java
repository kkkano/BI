package com.yupi.springbootinit.model.enums;

/**
 * Agent 执行阶段枚举（用于图表生成链路上下文追踪）
 */
public enum ChartTaskPhaseEnum {

    CREATED("created"),
    STATUS_RUNNING_UPDATED("status_running_updated"),
    AI_GENERATING("ai_generating"),
    AI_RESULT_PARSED("ai_result_parsed"),
    RESULT_PERSISTING("result_persisting"),
    RESULT_SUCCEED_UPDATED("result_succeed_updated"),
    FINISHED("finished"),
    FAILED("failed");

    private final String value;

    ChartTaskPhaseEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
