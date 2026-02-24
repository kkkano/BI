package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.impl.ChartServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ChartServiceImplTest {

    private final ChartServiceImpl chartService = new ChartServiceImpl();

    @Test
    void parseAiResultShouldReturnNullWhenInputIsBlank() {
        assertNull(chartService.parseAiResult(null));
        assertNull(chartService.parseAiResult("   "));
    }

    @Test
    void parseAiResultShouldReturnNullWhenDelimiterIsMissing() {
        String aiResult = "invalid format";
        assertNull(chartService.parseAiResult(aiResult));
    }

    @Test
    void parseAiResultShouldExtractChartAndConclusion() {
        String delimiter = ChartService.AI_RESULT_DELIMITER;
        String aiResult = "prefix" + delimiter + " {\"xAxis\":[]}" + delimiter + " analysis result ";

        String[] parsed = chartService.parseAiResult(aiResult);

        assertArrayEquals(new String[]{"{\"xAxis\":[]}", "analysis result"}, parsed);
    }

    @Test
    void parseAiResultShouldKeepExtraDelimiterInConclusion() {
        String delimiter = ChartService.AI_RESULT_DELIMITER;
        String aiResult = "prefix" + delimiter + "chart option" + delimiter + "partA" + delimiter + "partB";

        String[] parsed = chartService.parseAiResult(aiResult);

        assertArrayEquals(new String[]{"chart option", "partA" + delimiter + "partB"}, parsed);
    }

    @Test
    void getQueryWrapperShouldExcludeChartDataByDefault() {
        ChartQueryRequest request = new ChartQueryRequest();

        QueryWrapper<Chart> wrapper = chartService.getQueryWrapper(request);

        String sqlSelect = wrapper.getSqlSelect();
        assertTrue(sqlSelect.contains("genChart"));
        assertFalse(sqlSelect.contains("chartData"));
    }

    @Test
    void getQueryWrapperShouldIncludeChartDataWhenNeedChartDataIsTrue() {
        ChartQueryRequest request = new ChartQueryRequest();
        request.setNeedChartData(true);

        QueryWrapper<Chart> wrapper = chartService.getQueryWrapper(request);

        assertNull(wrapper.getSqlSelect());
    }

    @Test
    void getQueryWrapperShouldNormalizeStatusFilterToLowerCase() {
        ChartQueryRequest request = new ChartQueryRequest();
        request.setStatus(" Running ");

        QueryWrapper<Chart> wrapper = chartService.getQueryWrapper(request);

        assertTrue(wrapper.getParamNameValuePairs().containsValue("running"));
    }

    @Test
    void getQueryWrapperShouldRejectUnknownStatusFilter() {
        ChartQueryRequest request = new ChartQueryRequest();
        request.setStatus("processing");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartService.getQueryWrapper(request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("任务状态非法", exception.getMessage());
    }

    @Test
    void getQueryWrapperShouldUseFuzzyMatchForGoal() {
        ChartQueryRequest request = new ChartQueryRequest();
        request.setGoal(" 销量趋势 ");

        QueryWrapper<Chart> wrapper = chartService.getQueryWrapper(request);

        String sqlSegment = wrapper.getCustomSqlSegment();
        assertTrue(sqlSegment.toLowerCase().contains("goal like"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("销量趋势"));
    }

    @Test
    void generateAndPersistResultShouldMarkFailedWhenAiGenerationFails() {
        ChartServiceImpl serviceSpy = spy(new ChartServiceImpl());
        doReturn(null).when(serviceSpy).generateAndParseChartResult("input");
        doNothing().when(serviceSpy).handleChartUpdateError(eq(12L), anyString());

        String[] result = serviceSpy.generateAndPersistResult(12L, "input");

        assertNull(result);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(serviceSpy).handleChartUpdateError(eq(12L), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains(String.valueOf(ErrorCode.CHART_TASK_AI_GENERATE_FAILED.getCode())));
    }

    @Test
    void generateAndPersistResultShouldMarkFailedWhenPersistSucceedResultFails() {
        ChartServiceImpl serviceSpy = spy(new ChartServiceImpl());
        doReturn(new String[]{"genChart", "genResult"}).when(serviceSpy).generateAndParseChartResult("input");
        doReturn(false).when(serviceSpy).updateChartResultToSucceed(12L, "genChart", "genResult");
        doNothing().when(serviceSpy).handleChartUpdateError(eq(12L), anyString());

        String[] result = serviceSpy.generateAndPersistResult(12L, "input");

        assertNull(result);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(serviceSpy).handleChartUpdateError(eq(12L), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains(String.valueOf(ErrorCode.CHART_TASK_SUCCEED_UPDATE_FAILED.getCode())));
    }

    @Test
    void generateAndPersistResultShouldNotMarkFailedWhenPersistSucceeds() {
        ChartServiceImpl serviceSpy = spy(new ChartServiceImpl());
        doReturn(new String[]{"genChart", "genResult"}).when(serviceSpy).generateAndParseChartResult("input");
        doReturn(true).when(serviceSpy).updateChartResultToSucceed(12L, "genChart", "genResult");

        String[] result = serviceSpy.generateAndPersistResult(12L, "input");

        assertArrayEquals(new String[]{"genChart", "genResult"}, result);
        verify(serviceSpy, never()).handleChartUpdateError(eq(12L), anyString());
    }

    @Test
    void handleChartUpdateErrorShouldKeepStructuredErrorType() {
        ChartServiceImpl serviceSpy = spy(new ChartServiceImpl());
        doReturn(true).when(serviceSpy).update(any(Chart.class), any(Wrapper.class));

        serviceSpy.handleChartUpdateError(
                7L,
                "chartId=7 | errorType=50014 | timestamp=2026-02-24T13:30:00 | message=执行异常"
        );

        ArgumentCaptor<Chart> chartCaptor = ArgumentCaptor.forClass(Chart.class);
        verify(serviceSpy).update(chartCaptor.capture(), any(Wrapper.class));
        assertTrue(chartCaptor.getValue().getExecMessage().contains("errorType=50014"));
    }

    @Test
    void handleChartUpdateErrorShouldExtractPrefixErrorCode() {
        ChartServiceImpl serviceSpy = spy(new ChartServiceImpl());
        doReturn(true).when(serviceSpy).update(any(Chart.class), any(Wrapper.class));

        serviceSpy.handleChartUpdateError(7L, "50012: AI 生成图表结果失败");

        ArgumentCaptor<Chart> chartCaptor = ArgumentCaptor.forClass(Chart.class);
        verify(serviceSpy).update(chartCaptor.capture(), any(Wrapper.class));
        assertTrue(chartCaptor.getValue().getExecMessage().contains("errorType=50012"));
    }

    @Test
    void handleChartUpdateErrorShouldFallbackToSystemErrorCodeWhenMissing() {
        ChartServiceImpl serviceSpy = spy(new ChartServiceImpl());
        doReturn(true).when(serviceSpy).update(any(Chart.class), any(Wrapper.class));

        serviceSpy.handleChartUpdateError(7L, "执行异常");

        ArgumentCaptor<Chart> chartCaptor = ArgumentCaptor.forClass(Chart.class);
        verify(serviceSpy).update(chartCaptor.capture(), any(Wrapper.class));
        assertTrue(
                chartCaptor
                        .getValue()
                        .getExecMessage()
                        .contains("errorType=" + ErrorCode.SYSTEM_ERROR.getCode())
        );
    }
}
