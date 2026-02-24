package com.yupi.springbootinit.utils;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.model.enums.ChartTaskPhaseEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ChartTaskTraceUtilsTest {

    @Test
    void resolveTaskPhase_shouldPreferAgentPhaseFromStructuredFragment() {
        String execMessage = "chartId=1 | agentPhase=" + ChartTaskPhaseEnum.RESULT_PERSISTING.getValue() + " | message=test";

        String taskPhase = ChartTaskTraceUtils.resolveTaskPhase(ChartStatusEnum.RUNNING.getValue(), execMessage);

        Assertions.assertEquals(ChartTaskPhaseEnum.RESULT_PERSISTING.getValue(), taskPhase);
    }

    @Test
    void resolveTaskPhase_shouldIgnoreAgentPhaseSubstringInsideMessage() {
        String execMessage = "chartId=1 | message=下游提示 agentPhase=failed 但并非结构化字段";

        String taskPhase = ChartTaskTraceUtils.resolveTaskPhase(ChartStatusEnum.WAIT.getValue(), execMessage);

        Assertions.assertEquals(ChartTaskPhaseEnum.CREATED.getValue(), taskPhase);
    }

    @Test
    void parseFailureInfo_shouldReadStructuredFields() {
        String execMessage = "chartId=12 | errorType=50014 | timestamp=2026-02-24 06:20:00 | message=CSV 字段缺失";

        ChartTaskTraceUtils.TaskFailureInfo failureInfo =
                ChartTaskTraceUtils.parseFailureInfo(ChartStatusEnum.FAILED.getValue(), execMessage);

        Assertions.assertEquals("50014", failureInfo.getFailureCode());
        Assertions.assertEquals("CSV 字段缺失", failureInfo.getFailureMessage());
        Assertions.assertEquals("2026-02-24 06:20:00", failureInfo.getFailureTime());
    }

    @Test
    void parseFailureInfo_shouldNotParseTimestampFromMessageBody() {
        String execMessage = "chartId=12 | errorType=50014 | message=执行异常 timestamp=fake-time";

        ChartTaskTraceUtils.TaskFailureInfo failureInfo =
                ChartTaskTraceUtils.parseFailureInfo(ChartStatusEnum.FAILED.getValue(), execMessage);

        Assertions.assertEquals("50014", failureInfo.getFailureCode());
        Assertions.assertEquals("执行异常 timestamp=fake-time", failureInfo.getFailureMessage());
        Assertions.assertNull(failureInfo.getFailureTime());
    }

    @Test
    void parseFailureInfo_shouldKeepPipeTextWhenNotStructuredField() {
        String execMessage = "chartId=12 | errorType=50014 | message=CSV 字段缺失 | 请检查上传文件";

        ChartTaskTraceUtils.TaskFailureInfo failureInfo =
                ChartTaskTraceUtils.parseFailureInfo(ChartStatusEnum.FAILED.getValue(), execMessage);

        Assertions.assertEquals("50014", failureInfo.getFailureCode());
        Assertions.assertEquals("CSV 字段缺失 | 请检查上传文件", failureInfo.getFailureMessage());
    }

    @Test
    void parseFailureInfo_shouldExcludeTrailingAgentContextFromMessage() {
        String execMessage = "chartId=12 | errorType=50014 | message=CSV 字段缺失 | agentExecId=exec-1 | agentPhase=failed";

        ChartTaskTraceUtils.TaskFailureInfo failureInfo =
                ChartTaskTraceUtils.parseFailureInfo(ChartStatusEnum.FAILED.getValue(), execMessage);

        Assertions.assertEquals("CSV 字段缺失", failureInfo.getFailureMessage());
    }

    @Test
    void parseFailureInfo_shouldUseDefaultCodeAndMessageWhenExecMessageBlank() {
        ChartTaskTraceUtils.TaskFailureInfo failureInfo =
                ChartTaskTraceUtils.parseFailureInfo(ChartStatusEnum.FAILED.getValue(), "   ");

        Assertions.assertEquals(String.valueOf(ErrorCode.SYSTEM_ERROR.getCode()), failureInfo.getFailureCode());
        Assertions.assertEquals(ErrorCode.SYSTEM_ERROR.getMessage(), failureInfo.getFailureMessage());
        Assertions.assertNull(failureInfo.getFailureTime());
    }

    @Test
    void parseFailureInfo_shouldReturnEmptyWhenTaskNotFailed() {
        ChartTaskTraceUtils.TaskFailureInfo failureInfo =
                ChartTaskTraceUtils.parseFailureInfo(ChartStatusEnum.RUNNING.getValue(), "errorType=50014");

        Assertions.assertNull(failureInfo.getFailureCode());
        Assertions.assertNull(failureInfo.getFailureMessage());
        Assertions.assertNull(failureInfo.getFailureTime());
    }
}
