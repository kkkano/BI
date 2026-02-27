package com.yupi.springbootinit.utils;

import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * 图表任务状态展示工具（用于接口返回统一展示字段）
 */
public final class ChartTaskStatusViewUtils {

    private static final int PROGRESS_UNKNOWN = 0;
    private static final int PROGRESS_WAIT = 20;
    private static final int PROGRESS_RUNNING_DEFAULT = 60;
    private static final int PROGRESS_TERMINAL = 100;

    private ChartTaskStatusViewUtils() {
    }

    /**
     * 任务状态中文文案
     */
    public static String resolveStatusText(String status) {
        ChartStatusEnum statusEnum = ChartStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            return "未知状态";
        }
        return statusEnum.getText();
    }

    /**
     * 是否终态（succeed / failed）
     */
    public static boolean isTerminal(String status) {
        return ChartStatusEnum.SUCCEED.getValue().equals(status)
                || ChartStatusEnum.FAILED.getValue().equals(status);
    }

    /**
     * 当前状态是否建议继续轮询（wait / running）
     */
    public static boolean shouldPoll(String status) {
        return ChartStatusEnum.WAIT.getValue().equals(status)
                || ChartStatusEnum.RUNNING.getValue().equals(status);
    }

    /**
     * 任务进度（0~100），结合状态与阶段返回更细粒度的前端展示值
     */
    public static int resolveProgress(String status, String taskPhase) {
        if (ChartStatusEnum.WAIT.getValue().equals(status)) {
            return PROGRESS_WAIT;
        }
        if (isTerminal(status)) {
            return PROGRESS_TERMINAL;
        }
        if (!ChartStatusEnum.RUNNING.getValue().equals(status)) {
            return PROGRESS_UNKNOWN;
        }

        String normalizedTaskPhase = StringUtils.trimToEmpty(taskPhase);
        switch (normalizedTaskPhase) {
            case "status_running_updated":
                return 35;
            case "ai_generating":
                return 55;
            case "ai_result_parsed":
                return 75;
            case "result_persisting":
                return 90;
            case "result_succeed_updated":
            case "finished":
                return 95;
            default:
                return PROGRESS_RUNNING_DEFAULT;
        }
    }
}
