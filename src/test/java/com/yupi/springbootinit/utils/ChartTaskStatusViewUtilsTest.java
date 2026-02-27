package com.yupi.springbootinit.utils;

import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ChartTaskStatusViewUtilsTest {

    @Test
    void resolveStatusTextShouldReturnEnumText() {
        Assertions.assertEquals("排队中", ChartTaskStatusViewUtils.resolveStatusText(ChartStatusEnum.WAIT.getValue()));
        Assertions.assertEquals("未知状态", ChartTaskStatusViewUtils.resolveStatusText("unknown"));
    }

    @Test
    void terminalAndPollingShouldMatchStatus() {
        Assertions.assertTrue(ChartTaskStatusViewUtils.isTerminal(ChartStatusEnum.SUCCEED.getValue()));
        Assertions.assertTrue(ChartTaskStatusViewUtils.isTerminal(ChartStatusEnum.FAILED.getValue()));
        Assertions.assertFalse(ChartTaskStatusViewUtils.isTerminal(ChartStatusEnum.RUNNING.getValue()));

        Assertions.assertTrue(ChartTaskStatusViewUtils.shouldPoll(ChartStatusEnum.WAIT.getValue()));
        Assertions.assertTrue(ChartTaskStatusViewUtils.shouldPoll(ChartStatusEnum.RUNNING.getValue()));
        Assertions.assertFalse(ChartTaskStatusViewUtils.shouldPoll(ChartStatusEnum.SUCCEED.getValue()));
    }

    @Test
    void resolveProgressShouldReturnExpectedValues() {
        Assertions.assertEquals(20,
                ChartTaskStatusViewUtils.resolveProgress(ChartStatusEnum.WAIT.getValue(), "created"));
        Assertions.assertEquals(35,
                ChartTaskStatusViewUtils.resolveProgress(ChartStatusEnum.RUNNING.getValue(), "status_running_updated"));
        Assertions.assertEquals(55,
                ChartTaskStatusViewUtils.resolveProgress(ChartStatusEnum.RUNNING.getValue(), "ai_generating"));
        Assertions.assertEquals(75,
                ChartTaskStatusViewUtils.resolveProgress(ChartStatusEnum.RUNNING.getValue(), "ai_result_parsed"));
        Assertions.assertEquals(90,
                ChartTaskStatusViewUtils.resolveProgress(ChartStatusEnum.RUNNING.getValue(), "result_persisting"));
        Assertions.assertEquals(95,
                ChartTaskStatusViewUtils.resolveProgress(ChartStatusEnum.RUNNING.getValue(), "finished"));
        Assertions.assertEquals(60,
                ChartTaskStatusViewUtils.resolveProgress(ChartStatusEnum.RUNNING.getValue(), "unknown_phase"));
        Assertions.assertEquals(100,
                ChartTaskStatusViewUtils.resolveProgress(ChartStatusEnum.SUCCEED.getValue(), "finished"));
        Assertions.assertEquals(0,
                ChartTaskStatusViewUtils.resolveProgress("unknown", "created"));
    }
}
