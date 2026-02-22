package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.yupi.springbootinit.model.entity.Chart;

/**
 * 图表服务接口
 */
public interface ChartService extends IService<Chart> {

    /**
     * 构建 AI 分析所需的用户输入字符串
     *
     * @param goal      分析目标
     * @param chartType 图表类型（可为空）
     * @param csvData   CSV 格式的原始数据
     * @return 组装好的用户输入字符串
     */
    String buildUserInput(String goal, String chartType, String csvData);

    /**
     * 获取图表查询条件包装类
     *
     * @param chartQueryRequest 查询请求
     * @return MyBatis-Plus QueryWrapper
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

    /**
     * 处理图表状态更新失败（将图表状态设为 FAILED）
     *
     * @param chartId     图表 ID
     * @param execMessage 错误信息
     */
    void handleChartUpdateError(long chartId, String execMessage);
}
