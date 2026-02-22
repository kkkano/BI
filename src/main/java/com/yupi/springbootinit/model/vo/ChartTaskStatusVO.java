package com.yupi.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;

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

    private String execMessage;

    private String genChart;

    private String genResult;

    private static final long serialVersionUID = 1L;
}
