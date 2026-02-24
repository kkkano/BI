package com.yupi.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 图表任务批量状态响应（包含缺失任务信息）
 */
@Data
public class ChartTaskStatusBatchVO implements Serializable {

    /** 请求中的图表 id 原始数量（未去重） */
    private Integer rawRequestedCount;

    /** 请求中的图表 id 数量（去重后） */
    private Integer requestedCount;

    /** 去重后识别出的重复图表 id 数量 */
    private Integer duplicateCount;

    /** 重复出现的图表 id（去重后，按首次识别顺序返回） */
    private List<Long> duplicateChartIds = new ArrayList<>();

    /** 成功返回任务状态的数量 */
    private Integer returnedCount;

    /** 未返回任务状态的数量 */
    private Integer unavailableCount;

    /**
     * 未返回任务状态的图表 id（不存在 / 无权限 / 已删除）
     * 对普通用户统一视为不可用，避免泄露他人任务存在性
     */
    private List<Long> unavailableChartIds = new ArrayList<>();

    /** 任务状态列表（保持与请求顺序一致） */
    private List<ChartTaskStatusVO> taskStatusList = new ArrayList<>();

    private static final long serialVersionUID = 1L;
}
