package com.yupi.springbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.mapper.ChartMapper;
import com.yupi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 图表服务实现
 */
@Slf4j
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Override
    public String buildUserInput(String goal, String chartType, String csvData) {
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }

    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChart = buildChartStatusUpdate(chartId, ChartStatusEnum.FAILED);
        updateChart.setExecMessage(execMessage);
        boolean updated = updateById(updateChart);
        if (!updated) {
            log.error("更新图表失败状态失败 chartId={}, execMessage={}", chartId, execMessage);
        }
    }

    @Override
    public boolean updateChartStatusToRunning(long chartId) {
        return updateById(buildChartStatusUpdate(chartId, ChartStatusEnum.RUNNING));
    }

    @Override
    public boolean updateChartResultToSucceed(long chartId, String genChart, String genResult) {
        Chart updateChartResult = buildChartStatusUpdate(chartId, ChartStatusEnum.SUCCEED);
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        return updateById(updateChartResult);
    }

    @Override
    public boolean executeChartGeneration(long chartId, String userInput) {
        boolean runningUpdated = updateChartStatusToRunning(chartId);
        if (!runningUpdated) {
            handleChartUpdateError(chartId, "更新图表执行中状态失败");
            return false;
        }

        String[] parsedResult = generateChartAndResult(userInput);
        if (parsedResult == null) {
            handleChartUpdateError(chartId, "AI 生成错误");
            return false;
        }

        boolean succeedUpdated = updateChartResultToSucceed(chartId, parsedResult[0], parsedResult[1]);
        if (!succeedUpdated) {
            handleChartUpdateError(chartId, "更新图表成功状态失败");
            return false;
        }
        return true;
    }

    @Override
    public String[] generateChartAndResult(String userInput) {
        String aiResult = aiManager.doChat(CommonConstant.BI_MODEL_ID, userInput);
        return parseAiResult(aiResult);
    }

    @Override
    public String[] parseAiResult(String aiResult) {
        String[] splits = aiResult.split(AI_RESULT_DELIMITER);
        if (splits.length < 3) {
            return null;
        }
        return new String[]{splits[1].trim(), splits[2].trim()};
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChartAndDeductPoint(Chart chart, User loginUser) {
        boolean saveResult = save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        userService.updateUserPointsAndUsageCount(loginUser);
    }

    @Override
    public Chart buildWaitChart(String name, String goal, String chartType, String csvData, Long userId) {
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(ChartStatusEnum.WAIT.getValue());
        chart.setUserId(userId);
        return chart;
    }

    @Override
    public void saveWaitChart(Chart chart) {
        boolean saveResult = save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
    }

    private Chart buildChartStatusUpdate(long chartId, ChartStatusEnum statusEnum) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(statusEnum.getValue());
        return updateChart;
    }
}
