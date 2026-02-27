package com.yupi.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图表任务状态响应
 */
@Data
public class ChartTaskStatusVO implements Serializable {

    private Long chartId;

    /** 图表名称 */
    private String name;

    /** 分析目标 */
    private String goal;

    /** 图表类型 */
    private String chartType;

    /** wait / running / succeed / failed */
    private String status;

    /** 状态文案（排队中 / 执行中 / 已完成 / 已失败） */
    private String statusText;

    /** created / status_running_updated / ai_generating / ... */
    private String taskPhase;

    /** 可用于串联任务日志的追踪 id */
    private String traceId;

    /** 当前状态是否建议继续轮询（wait / running 为 true） */
    private Boolean shouldPoll;

    /** 当前状态是否为终态（succeed / failed 为 true） */
    private Boolean terminal;

    /** 任务展示进度（0~100） */
    private Integer progress;

    private String execMessage;

    /** 失败码（仅 failed 场景） */
    private String failureCode;

    /** 失败原因（仅 failed 场景） */
    private String failureReason;

    /** 失败时间（仅 failed 场景） */
    private String failureTime;

    private String genChart;

    private String genResult;

    /** 任务创建时间 */
    private Date createTime;

    /** 任务最近更新时间 */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
