package com.yupi.springbootinit.utils;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.model.enums.ChartTaskPhaseEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task trace helper for async chart generation.
 */
public final class ChartTaskTraceUtils {

    private static final String TRACE_ID_PREFIX = "chart-task-";
    /**
     * 匹配结构化片段中的 key=value 起始位置。
     *
     * <p>示例：
     * chartId=1 | errorType=50014 | message=CSV 缺失 | agentPhase=failed
     */
    private static final Pattern STRUCTURED_FIELD_PATTERN =
            Pattern.compile("(?:^|\\|)\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*=\\s*");

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
        Map<String, String> fieldMap = parseStructuredFields(execMessage);
        String agentPhase = fieldMap.get("agentPhase");
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

        Map<String, String> fieldMap = parseStructuredFields(execMessage);

        String failureCode = fieldMap.get("errorType");
        if (StringUtils.isBlank(failureCode)) {
            failureCode = defaultFailureCode;
        }
        String failureTime = fieldMap.get("timestamp");
        String failureMessage = fieldMap.get("message");
        if (StringUtils.isBlank(failureMessage)) {
            failureMessage = execMessage;
        }
        return new TaskFailureInfo(failureCode, failureMessage, failureTime);
    }

    /**
     * 将 execMessage 解析为结构化字段。
     *
     * <p>相比按 "|" 直接 split，此实现允许 value 内包含普通 "|" 片段，
     * 只有在后续片段满足「key=」形态时才判定为新字段起点。
     */
    private static Map<String, String> parseStructuredFields(String content) {
        if (StringUtils.isBlank(content)) {
            return new HashMap<>();
        }

        Map<String, String> fieldMap = new HashMap<>();
        Matcher matcher = STRUCTURED_FIELD_PATTERN.matcher(content);
        String currentKey = null;
        int valueStart = -1;
        while (matcher.find()) {
            if (currentKey != null && valueStart >= 0) {
                String value = StringUtils.trimToNull(content.substring(valueStart, matcher.start()));
                fieldMap.put(currentKey, value);
            }
            currentKey = matcher.group(1);
            valueStart = matcher.end();
        }

        if (currentKey != null && valueStart >= 0 && valueStart <= content.length()) {
            String value = StringUtils.trimToNull(content.substring(valueStart));
            fieldMap.put(currentKey, value);
        }
        return fieldMap;
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
