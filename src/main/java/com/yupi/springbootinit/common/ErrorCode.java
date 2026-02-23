package com.yupi.springbootinit.common;

/**
 * 自定义错误码
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    TOO_MANY_REQUEST(42900, "请求过于频繁"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    CHART_TASK_REJECTED(50010, "图表任务提交失败，系统繁忙"),
    CHART_TASK_RUNNING_UPDATE_FAILED(50011, "图表任务状态更新失败（执行中）"),
    CHART_TASK_AI_GENERATE_FAILED(50012, "AI 生成图表结果失败"),
    CHART_TASK_SUCCEED_UPDATE_FAILED(50013, "图表任务状态更新失败（已完成）"),
    CHART_TASK_EXECUTE_EXCEPTION(50014, "图表任务执行异常");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
