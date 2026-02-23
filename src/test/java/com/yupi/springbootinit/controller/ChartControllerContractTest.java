package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChartController.class)
class ChartControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChartService chartService;

    @MockBean
    private UserService userService;

    @MockBean
    private RedisLimiterManager redisLimiterManager;

    @MockBean
    private ThreadPoolExecutor threadPoolExecutor;

    @MockBean
    private BiMessageProducer biMessageProducer;

    @BeforeEach
    void setUp() {
        User loginUser = new User();
        loginUser.setId(99L);
        loginUser.setPoints(10);
        when(userService.getLoginUser(any())).thenReturn(loginUser);

        when(threadPoolExecutor.getActiveCount()).thenReturn(0);
        when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(1);
        when(threadPoolExecutor.getQueue()).thenReturn(new LinkedBlockingQueue<>(1));

        when(chartService.buildUserInput(anyString(), anyString(), anyString())).thenReturn("mock-user-input");
    }

    @Test
    void genChartAsync_shouldFollowResponseContract() throws Exception {
        BiResponse biResponse = buildBiResponse(1001L, "wait", "任务排队中");
        when(chartService.createAsyncThreadTask(any(), any())).thenReturn(biResponse);

        mockMvc.perform(multipart("/chart/gen/async")
                        .file(mockCsvFile())
                        .param("name", "销量分析")
                        .param("goal", "分析销量趋势")
                        .param("chartType", "折线图"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.chartId", anyOf(is(1001), is("1001"))))
                .andExpect(jsonPath("$.data.status").value("wait"))
                .andExpect(jsonPath("$.data.execMessage").value("任务排队中"))
                .andExpect(jsonPath("$.data.createTime").exists())
                .andExpect(jsonPath("$.data.updateTime").exists());
    }

    @Test
    void genChartAsyncMq_shouldExposeSucceedStatusAndTimeFields() throws Exception {
        BiResponse biResponse = buildBiResponse(2002L, "succeed", "图表生成完成");
        when(chartService.createAsyncMqTask(any(), any())).thenReturn(biResponse);

        mockMvc.perform(multipart("/chart/gen/async/mq")
                        .file(mockCsvFile())
                        .param("name", "销量分析")
                        .param("goal", "分析销量趋势")
                        .param("chartType", "折线图"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.chartId", anyOf(is(2002), is("2002"))))
                .andExpect(jsonPath("$.data.status").value("succeed"))
                .andExpect(jsonPath("$.data.execMessage").value("图表生成完成"))
                .andExpect(jsonPath("$.data.createTime").exists())
                .andExpect(jsonPath("$.data.updateTime").exists());

        verify(biMessageProducer).sendMessage("2002");
    }

    private BiResponse buildBiResponse(long chartId, String status, String execMessage) {
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        biResponse.setName("销量分析");
        biResponse.setGoal("分析销量趋势");
        biResponse.setChartType("折线图");
        biResponse.setStatus(status);
        biResponse.setExecMessage(execMessage);
        biResponse.setCreateTime(new Date());
        biResponse.setUpdateTime(new Date());
        return biResponse;
    }

    private MockMultipartFile mockCsvFile() {
        String csv = "日期,销量\n1月,120\n";
        return new MockMultipartFile(
                "file",
                "data.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
    }
}
