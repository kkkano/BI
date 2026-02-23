package com.yupi.springbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.mapper.ChartMapper;
import com.yupi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.yupi.springbootinit.model.dto.chart.GenChartRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

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
        String status = chartQueryRequest.getStatus();
        Long userId = chartQueryRequest.getUserId();
        Boolean needChartData = chartQueryRequest.getNeedChartData();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(StringUtils.isNotBlank(status), "status", status);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        if (!Boolean.TRUE.equals(needChartData)) {
            queryWrapper.select("id", "name", "goal", "chartType", "genChart", "genResult",
                    "status", "execMessage", "userId", "createTime", "updateTime");
        }
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    private static final DateTimeFormatter EXEC_MESSAGE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void handleChartUpdateError(long chartId, String execMessage) {
        String normalizedExecMessage = buildStandardExecMessage(chartId, execMessage);
        Chart updateChart = buildChartStatusUpdate(chartId, ChartStatusEnum.FAILED);
        updateChart.setExecMessage(normalizedExecMessage);
        boolean updated = updateById(updateChart);
        if (!updated) {
            log.error("图表状态更新失败 chartId={}, status={}, execMessage={}",
                    chartId, ChartStatusEnum.FAILED.getValue(), normalizedExecMessage);
        }
    }

    @Override
    public boolean updateChartStatusToRunning(long chartId) {
        boolean updated = updateById(buildChartStatusUpdate(chartId, ChartStatusEnum.RUNNING));
        if (!updated) {
            log.error("图表状态更新失败 chartId={}, status={}", chartId, ChartStatusEnum.RUNNING.getValue());
        }
        return updated;
    }

    @Override
    public boolean updateChartResultToSucceed(long chartId, String genChart, String genResult) {
        Chart updateChartResult = buildChartStatusUpdate(chartId, ChartStatusEnum.SUCCEED);
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        boolean updated = updateById(updateChartResult);
        if (!updated) {
            log.error("图表状态更新失败 chartId={}, status={}", chartId, ChartStatusEnum.SUCCEED.getValue());
        }
        return updated;
    }

    @Override
    public boolean executeChartGeneration(long chartId, String userInput) {
        boolean runningUpdated = updateChartStatusToRunning(chartId);
        if (!runningUpdated) {
            handleChartUpdateError(chartId,
                    ErrorCode.CHART_TASK_RUNNING_UPDATE_FAILED.getCode() + ": " +
                            ErrorCode.CHART_TASK_RUNNING_UPDATE_FAILED.getMessage());
            return false;
        }

        String[] parsedResult = generateAndParseChartResult(userInput);
        if (parsedResult == null) {
            handleChartUpdateError(chartId,
                    ErrorCode.CHART_TASK_AI_GENERATE_FAILED.getCode() + ": " +
                            ErrorCode.CHART_TASK_AI_GENERATE_FAILED.getMessage());
            return false;
        }

        boolean succeedUpdated = updateChartResultToSucceed(chartId, parsedResult[0], parsedResult[1]);
        if (!succeedUpdated) {
            handleChartUpdateError(chartId,
                    ErrorCode.CHART_TASK_SUCCEED_UPDATE_FAILED.getCode() + ": " +
                            ErrorCode.CHART_TASK_SUCCEED_UPDATE_FAILED.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public String[] generateAndParseChartResult(String userInput) {
        try {
            String aiResult = aiManager.doChat(CommonConstant.BI_MODEL_ID, userInput);
            return parseAiResult(aiResult);
        } catch (Exception e) {
            log.error("AI 调用失败 modelId={}, userInputLength={}",
                    CommonConstant.BI_MODEL_ID, StringUtils.length(userInput), e);
            return null;
        }
    }

    @Override
    public Chart createRunningChart(GenChartRequest req, User loginUser) {
        return createChartWithRunningStatus(req, loginUser);
    }

    @Override
    public String[] generateAndPersistResult(long chartId, String userInput) {
        String[] parsedResult = generateAndParseChartResult(userInput);
        if (parsedResult == null) {
            handleChartUpdateError(chartId,
                    ErrorCode.CHART_TASK_AI_GENERATE_FAILED.getCode() + ": " +
                            ErrorCode.CHART_TASK_AI_GENERATE_FAILED.getMessage());
            return null;
        }
        boolean updated = updateChartResultToSucceed(chartId, parsedResult[0], parsedResult[1]);
        if (!updated) {
            handleChartUpdateError(chartId,
                    ErrorCode.CHART_TASK_SUCCEED_UPDATE_FAILED.getCode() + ": " +
                            ErrorCode.CHART_TASK_SUCCEED_UPDATE_FAILED.getMessage());
            return null;
        }
        return parsedResult;
    }

    @Override
    public Chart createWaitChart(GenChartRequest req, User loginUser) {
        return createChartWithWaitStatus(req, loginUser);
    }

    @Override
    public Chart buildChart(String name, String goal, String chartType, String csvData, Long userId, ChartStatusEnum status) {
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(status.getValue());
        chart.setUserId(userId);
        return chart;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Chart createChartWithRunningStatus(GenChartRequest req, User loginUser) {
        Chart chart = buildChart(req.getName(), req.getGoal(), req.getChartType(), req.getCsvData(),
                loginUser.getId(), ChartStatusEnum.RUNNING);
        saveChartAndDeductPoint(chart, loginUser);
        return chart;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Chart createChartWithWaitStatus(GenChartRequest req, User loginUser) {
        Chart chart = buildChart(req.getName(), req.getGoal(), req.getChartType(), req.getCsvData(),
                loginUser.getId(), ChartStatusEnum.WAIT);
        saveChartAndDeductPoint(chart, loginUser);
        return chart;
    }

    @Override
    public BiResponse generateChartSync(GenChartRequest req, User loginUser) {
        Chart chart = createChartWithRunningStatus(req, loginUser);
        String userInput = buildUserInput(req.getGoal(), req.getChartType(), req.getCsvData());
        String[] parsedResult = generateAndPersistResult(chart.getId(), userInput);
        if (parsedResult == null) {
            return null;
        }
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        biResponse.setGenChart(parsedResult[0]);
        biResponse.setGenResult(parsedResult[1]);
        return biResponse;
    }

    @Override
    public BiResponse createAsyncThreadTask(GenChartRequest req, User loginUser) {
        Chart chart = createChartWithWaitStatus(req, loginUser);
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    @Override
    public BiResponse createAsyncMqTask(GenChartRequest req, User loginUser) {
        Chart chart = createChartWithWaitStatus(req, loginUser);
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    @Override
    public String[] parseAiResult(String aiResult) {
        if (StringUtils.isBlank(aiResult)) {
            log.warn("AI 返回结果为空，无法解析");
            return null;
        }
        String[] splits = aiResult.split(Pattern.quote(AI_RESULT_DELIMITER), 3);
        if (splits.length < 3 || StringUtils.isAnyBlank(splits[1], splits[2])) {
            log.warn("AI 返回结果格式异常 splitCount={}, aiResultLength={}, preview={}",
                    splits.length, aiResult.length(), StringUtils.abbreviate(aiResult, 200));
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
        return buildChart(name, goal, chartType, csvData, userId, ChartStatusEnum.WAIT);
    }

    @Override
    public void saveWaitChart(Chart chart) {
        boolean saveResult = save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
    }

    private String buildStandardExecMessage(long chartId, String execMessage) {
        String timestamp = LocalDateTime.now().format(EXEC_MESSAGE_TIME_FORMATTER);
        String errorType = extractErrorType(execMessage);
        String errorMessage = StringUtils.defaultIfBlank(execMessage, ErrorCode.SYSTEM_ERROR.getMessage());
        return String.format("chartId=%d | errorType=%s | timestamp=%s | message=%s",
                chartId, errorType, timestamp, errorMessage);
    }

    private String extractErrorType(String execMessage) {
        if (StringUtils.isBlank(execMessage)) {
            return ErrorCode.SYSTEM_ERROR.getCode();
        }
        int separatorIndex = execMessage.indexOf(":");
        if (separatorIndex <= 0) {
            return ErrorCode.SYSTEM_ERROR.getCode();
        }
        return execMessage.substring(0, separatorIndex).trim();
    }

    private Chart buildChartStatusUpdate(long chartId, ChartStatusEnum statusEnum) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(statusEnum.getValue());
        return updateChart;
    }
}
