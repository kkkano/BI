package com.yupi.springbootinit.utils;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.model.enums.ChartTaskPhaseEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 解析失败任务的结构化错误信息。
     */
    public static TaskFailureInfo parseFailureInfo(String status, String execMessage) {
        if (!ChartStatusEnum.FAILED.getValue().equals(status)) {
            return TaskFailureInfo.empty();
        }

        String defaultFailureCode = String.valueOf(ErrorCode.SYSTEM_ERROR.getCode());
        if (StringUtils.isBlank(execMessage)) {
            return new TaskFailureInfo(defaultFailureCode, ErrorCode.SYSTEM_ERROR.getMessage(), null);
        }

        String failureCode = extractFieldValue(execMessage, "errorType");
        if (StringUtils.isBlank(failureCode)) {
            failureCode = defaultFailureCode;
        }
        String failureTime = extractFieldValue(execMessage, "timestamp");
        String failureMessage = extractFieldValue(execMessage, "message");
        if (StringUtils.isBlank(failureMessage)) {
            failureMessage = execMessage;
        }
        return new TaskFailureInfo(failureCode, failureMessage, failureTime);
    }

    /**
     * 仅匹配格式化片段中的 key=value（在字符串开头或 | 分隔后），
     * 避免误命中 message 文本中的同名子串。
     */
    private static String extractFieldValue(String content, String key) {
        if (StringUtils.isBlank(content) || StringUtils.isBlank(key)) {
            return null;
        }
        Pattern fieldPattern = Pattern.compile("(?:^|\\|)\\s*" + Pattern.quote(key) + "\\s*=\\s*([^|]+)");
        Matcher matcher = fieldPattern.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return StringUtils.trimToNull(matcher.group(1));
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
