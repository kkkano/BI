package com.yupi.springbootinit.utils;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.model.enums.ChartTaskPhaseEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * Task trace helper for async chart generation.
 */
public final class ChartTaskTraceUtils {

    private static final String TRACE_ID_PREFIX = "chart-task-";

    private ChartTaskTraceUtils() {
    }

    public static String buildTraceId(Long chartId) {
        if (chartId == null || chartId <= 0) {
            return null;
        }
        return TRACE_ID_PREFIX + chartId;
    }

    public static String resolveTaskPhase(String status) {
        return resolveTaskPhase(status, null);
    }

    /**
     * 优先读取 execMessage 里的 agentPhase；缺失时按任务状态给出兼容阶段。
     */
    public static String resolveTaskPhase(String status, String execMessage) {
        String agentPhase = extractFieldValue(execMessage, "agentPhase");
        if (isKnownTaskPhase(agentPhase)) {
            return agentPhase;
        }
        if (ChartStatusEnum.WAIT.getValue().equals(status)) {
            return ChartTaskPhaseEnum.CREATED.getValue();
        }
        if (ChartStatusEnum.RUNNING.getValue().equals(status)) {
            return ChartTaskPhaseEnum.AI_GENERATING.getValue();
        }
        if (ChartStatusEnum.SUCCEED.getValue().equals(status)) {
            return ChartTaskPhaseEnum.FINISHED.getValue();
        }
        if (ChartStatusEnum.FAILED.getValue().equals(status)) {
            return ChartTaskPhaseEnum.FAILED.getValue();
        }
        return null;
    }

    public static TaskFailureInfo parseFailureInfo(String status, String execMessage) {
        if (!ChartStatusEnum.FAILED.getValue().equals(status)) {
            return TaskFailureInfo.empty();
        }
        if (StringUtils.isBlank(execMessage)) {
            return new TaskFailureInfo(ErrorCode.SYSTEM_ERROR.getCode(),
                    ErrorCode.SYSTEM_ERROR.getMessage(), null);
        }

        String failureCode = extractFieldValue(execMessage, "errorType");
        if (StringUtils.isBlank(failureCode)) {
            failureCode = ErrorCode.SYSTEM_ERROR.getCode();
        }
        String failureTime = extractFieldValue(execMessage, "timestamp");
        String failureMessage = extractFieldValue(execMessage, "message");
        if (StringUtils.isBlank(failureMessage)) {
            failureMessage = execMessage;
        }
        return new TaskFailureInfo(failureCode, failureMessage, failureTime);
    }

    private static String extractFieldValue(String content, String key) {
        if (StringUtils.isBlank(content) || StringUtils.isBlank(key)) {
            return null;
        }
        String marker = key + "=";
        int startIndex = content.indexOf(marker);
        if (startIndex < 0) {
            return null;
        }
        int valueStart = startIndex + marker.length();
        if (valueStart >= content.length()) {
            return null;
        }
        int valueEnd = content.indexOf('|', valueStart);
        if (valueEnd < 0) {
            valueEnd = content.length();
        }
        String value = StringUtils.trim(content.substring(valueStart, valueEnd));
        return StringUtils.isBlank(value) ? null : value;
    }

    private static boolean isKnownTaskPhase(String phase) {
        if (StringUtils.isBlank(phase)) {
            return false;
        }
        for (ChartTaskPhaseEnum phaseEnum : ChartTaskPhaseEnum.values()) {
            if (phaseEnum.getValue().equals(phase)) {
                return true;
            }
        }
        return false;
    }

    public static final class TaskFailureInfo {

        private final String failureCode;
        private final String failureMessage;
        private final String failureTime;

        private TaskFailureInfo(String failureCode, String failureMessage, String failureTime) {
            this.failureCode = failureCode;
            this.failureMessage = failureMessage;
            this.failureTime = failureTime;
        }

        public static TaskFailureInfo empty() {
            return new TaskFailureInfo(null, null, null);
        }

        public String getFailureCode() {
            return failureCode;
        }

        public String getFailureMessage() {
            return failureMessage;
        }

        public String getFailureTime() {
            return failureTime;
        }
    }
}
