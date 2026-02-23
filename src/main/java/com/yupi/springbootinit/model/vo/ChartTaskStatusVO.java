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

    /** queued / executing / completed / failed / unknown */
    private String taskPhase;

    /** 可用于串联任务日志的追踪 id */
    private String traceId;

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
