package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.impl.ChartServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
