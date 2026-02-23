package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.model.dto.chart.GenChartByAiRequest;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChartControllerGenRequestValidationTest {

    private ChartController chartController;

    @Mock
    private ChartService chartService;

    @Mock
    private UserService userService;

    @Mock
    private RedisLimiterManager redisLimiterManager;

    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    @Mock
    private BiMessageProducer biMessageProducer;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        chartController = new ChartController();
        ReflectionTestUtils.setField(chartController, "chartService", chartService);
        ReflectionTestUtils.setField(chartController, "userService", userService);
        ReflectionTestUtils.setField(chartController, "redisLimiterManager", redisLimiterManager);
        ReflectionTestUtils.setField(chartController, "threadPoolExecutor", threadPoolExecutor);
        ReflectionTestUtils.setField(chartController, "biMessageProducer", biMessageProducer);
    }

    @Test
    void genEndpointsShouldRejectNullGenRequest() {
        MockMultipartFile file = buildCsvFile();

        BusinessException syncException = assertThrows(BusinessException.class,
                () -> chartController.genChartByAi(file, null, request));
        BusinessException asyncException = assertThrows(BusinessException.class,
                () -> chartController.genChartByAiAsync(file, null, request));
        BusinessException asyncMqException = assertThrows(BusinessException.class,
                () -> chartController.genChartByAiAsyncMq(file, null, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), syncException.getCode());
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), asyncException.getCode());
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), asyncMqException.getCode());
        verifyNoInteractions(chartService, userService, redisLimiterManager, biMessageProducer);
    }

    @Test
    void genEndpointsShouldRejectBlankGoal() {
        MockMultipartFile file = buildCsvFile();
        GenChartByAiRequest requestBody = new GenChartByAiRequest();
        requestBody.setName("sales");
        requestBody.setGoal("   ");
        requestBody.setChartType("line");

        BusinessException syncException = assertThrows(BusinessException.class,
                () -> chartController.genChartByAi(file, requestBody, request));
        BusinessException asyncException = assertThrows(BusinessException.class,
                () -> chartController.genChartByAiAsync(file, requestBody, request));
        BusinessException asyncMqException = assertThrows(BusinessException.class,
                () -> chartController.genChartByAiAsyncMq(file, requestBody, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), syncException.getCode());
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), asyncException.getCode());
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), asyncMqException.getCode());
        verifyNoInteractions(chartService, userService, redisLimiterManager, biMessageProducer);
    }

    private MockMultipartFile buildCsvFile() {
        return new MockMultipartFile(
                "file",
                "demo.csv",
                "text/csv",
                "date,sales\n2026-01,10\n".getBytes(StandardCharsets.UTF_8));
    }
}
