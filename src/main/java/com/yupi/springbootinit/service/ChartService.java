package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;

/**
 * 图表服务接口
 */
public interface ChartService extends IService<Chart> {

    /**
     * AI 返回结果分隔符
     */
    String AI_RESULT_DELIMITER = "【【【【【";

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

    /**
     * 将图表状态更新为 RUNNING
     *
     * @param chartId 图表 ID
     * @return 是否更新成功
     */
    boolean updateChartStatusToRunning(long chartId);

    /**
     * 更新图表生成结果并将状态标记为 SUCCEED
     *
     * @param chartId   图表 ID
     * @param genChart  生成的图表配置
     * @param genResult 生成的分析结论
     * @return 是否更新成功
     */
    boolean updateChartResultToSucceed(long chartId, String genChart, String genResult);

    /**
     * 解析 AI 返回文本，提取图表配置和分析结论
     *
     * @param aiResult AI 原始返回结果
     * @return 长度为 2 的数组：index=0 为 genChart，index=1 为 genResult；解析失败返回 null
     */
    String[] parseAiResult(String aiResult);

    /**
     * 保存图表并扣减一次使用积分（事务保障）
     *
     * @param chart     图表实体
     * @param loginUser 当前登录用户
     */
    void saveChartAndDeductPoint(Chart chart, User loginUser);
}
