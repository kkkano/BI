package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.dto.chart.GenChartRequest;

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
     * 执行图表 AI 生成任务：更新为运行中 -> 调用 AI -> 解析并落库结果（失败时自动回写失败状态）
     *
     * @param chartId   图表 ID
     * @param userInput AI 输入内容
     * @return 是否执行成功
     */
    boolean executeChartGeneration(long chartId, String userInput);

    /**
     * 解析 AI 返回文本，提取图表配置和分析结论
     *
     * @param aiResult AI 原始返回结果
     * @return 长度为 2 的数组：index=0 为 genChart，index=1 为 genResult；解析失败返回 null
     */
    String[] parseAiResult(String aiResult);

    /**
     * 调用 AI 并解析结果
     *
     * @param userInput AI 输入内容
     * @return 长度为 2 的数组：index=0 为 genChart，index=1 为 genResult；失败返回 null
     */
    String[] generateAndParseChartResult(String userInput);

    /**
     * 解析请求并落库为运行中图表（同步模式）
     *
     * @param req       生成请求参数
     * @param loginUser 当前登录用户
     * @return 已落库图表
     */
    Chart createRunningChart(GenChartRequest req, User loginUser);

    /**
     * 同步执行 AI 生成并回填图表结果
     *
     * @param chartId   图表 ID
     * @param userInput AI 输入内容
     * @return 长度为 2 的数组：index=0 为 genChart，index=1 为 genResult；失败返回 null
     */
    String[] generateAndPersistResult(long chartId, String userInput);

    /**
     * 解析请求并落库为等待中图表（异步模式）
     *
     * @param req       生成请求参数
     * @param loginUser 当前登录用户
     * @return 已落库图表
     */
    Chart createWaitChart(GenChartRequest req, User loginUser);

    /**
     * 保存图表并扣减一次使用积分（事务保障）
     *
     * @param chart     图表实体
     * @param loginUser 当前登录用户
     */
    void saveChartAndDeductPoint(Chart chart, User loginUser);

    /**
     * 构造待入库的图表实体（等待状态）
     *
     * @param name      图表名称
     * @param goal      分析目标
     * @param chartType 图表类型
     * @param csvData   原始数据
     * @param userId    用户 ID
     * @return 待保存图表
     */
    Chart buildWaitChart(String name, String goal, String chartType, String csvData, Long userId);

    /**
     * 保存等待中的图表记录
     *
     * @param chart 图表实体
     */
    void saveWaitChart(Chart chart);
}
