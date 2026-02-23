package com.yupi.springbootinit.service;

import com.yupi.springbootinit.service.impl.ChartServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
