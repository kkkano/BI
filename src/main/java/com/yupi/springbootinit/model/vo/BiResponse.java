package com.yupi.springbootinit.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * Bi 的返回结果
 */
@Data
public class BiResponse {

    /** 图表 id */
    private Long chartId;

    /** 图表名称 */
    private String name;

    /** 分析目标 */
    private String goal;

    /** 图表类型 */
    private String chartType;

    /** 任务状态：wait / running / succeed / failed */
    private String status;

    /** 状态文案（排队中 / 执行中 / 已完成 / 已失败） */
    private String statusText;

    /** 任务阶段：created / status_running_updated / ai_generating / ... */
    private String taskPhase;

    /** 任务追踪 id，可用于前后端日志串联 */
    private String traceId;

    /** 当前状态是否建议继续轮询（wait / running 为 true） */
    private Boolean shouldPoll;

    /** 当前状态是否为终态（succeed / failed 为 true） */
    private Boolean terminal;

    /** 任务展示进度（0~100） */
    private Integer progress;

    /** 任务执行信息 */
    private String execMessage;

    /** 失败码（仅 failed 场景） */
    private String failureCode;

    /** 失败原因（仅 failed 场景） */
    private String failureReason;

    /** 失败时间（仅 failed 场景） */
    private String failureTime;

    /** 生成的图表配置 */
    private String genChart;

    /** 生成的分析结论 */
    private String genResult;

    /** 任务创建时间 */
    private Date createTime;

    /** 任务更新时间 */
    private Date updateTime;
}
