package com.yupi.springbootinit.exception;

import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.RejectedExecutionException;

/**
 * 全局异常处理器
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href="https://github.com/kkkano/BI"</a>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 线程池任务队列已满时抛出，返回更友好的提示
     * 异步接口注释说"由全局异常处理器兜底"，此处补全对应处理
     */
    @ExceptionHandler(RejectedExecutionException.class)
    public BaseResponse<?> rejectedExecutionExceptionHandler(RejectedExecutionException e) {
        log.error("RejectedExecutionException: 任务队列已满", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "当前请求繁忙，请稍后再试");
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
