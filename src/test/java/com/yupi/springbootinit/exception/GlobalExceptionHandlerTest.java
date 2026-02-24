package com.yupi.springbootinit.exception;

import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void rejectedExecutionExceptionShouldReturnChartTaskRejectedCode() {
        BaseResponse<?> response = globalExceptionHandler
                .rejectedExecutionExceptionHandler(new RejectedExecutionException("executor is full"));

        assertEquals(ErrorCode.CHART_TASK_REJECTED.getCode(), response.getCode());
        assertEquals(ErrorCode.CHART_TASK_REJECTED.getMessage(), response.getMessage());
    }

    @Test
    void asyncWrappedExceptionShouldUnwrapNestedBusinessException() {
        BusinessException businessException = new BusinessException(ErrorCode.PARAMS_ERROR, "图表 id 非法");
        CompletionException completionException = new CompletionException(new RuntimeException(businessException));

        BaseResponse<?> response = globalExceptionHandler.asyncWrappedExceptionHandler(completionException);

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), response.getCode());
        assertEquals("图表 id 非法", response.getMessage());
    }

    @Test
    void httpMessageNotReadableShouldReturnParamsError() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "json parse failed",
                new MockHttpInputMessage("{\"goal\":123}".getBytes(StandardCharsets.UTF_8)));

        BaseResponse<?> response = globalExceptionHandler.httpMessageNotReadableExceptionHandler(exception);

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), response.getCode());
        assertEquals("请求体格式错误，请检查 JSON 字段类型与结构", response.getMessage());
    }

    @Test
    void illegalArgumentExceptionShouldKeepOriginalMessage() {
        BaseResponse<?> response = globalExceptionHandler
                .illegalArgumentExceptionHandler(new IllegalArgumentException("chartId 必须大于 0"));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), response.getCode());
        assertEquals("chartId 必须大于 0", response.getMessage());
    }
}
