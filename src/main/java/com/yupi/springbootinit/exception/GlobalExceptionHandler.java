package com.yupi.springbootinit.exception;

import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

/**
 * 全局异常处理器
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href="https://github.com/kkkano/BI"></a>
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
     * 处理 @Valid 请求体校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException", e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    /**
     * 处理 form/query 参数校验失败
     */
    @ExceptionHandler(BindException.class)
    public BaseResponse<?> bindExceptionHandler(BindException e) {
        log.warn("BindException", e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    /**
     * 处理方法参数约束校验失败（如 @RequestParam / @PathVariable）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<?> constraintViolationExceptionHandler(ConstraintViolationException e) {
        log.warn("ConstraintViolationException", e);
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    /**
     * query/form 缺少必须参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public BaseResponse<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        log.warn("MissingServletRequestParameterException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "缺少必要参数：" + e.getParameterName());
    }

    /**
     * 路径参数 / 查询参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public BaseResponse<?> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException", e);
        String name = e.getName() == null ? "参数" : e.getName();
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, name + " 参数类型错误");
    }

    /**
     * 请求体 JSON 无法解析（字段类型错误、JSON 结构错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请求体格式错误，请检查 JSON 字段类型与结构");
    }

    /**
     * 参数语义非法（用于兜底 IllegalArgumentException）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<?> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.warn("IllegalArgumentException", e);
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = ErrorCode.PARAMS_ERROR.getMessage();
        }
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public BaseResponse<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请求方法不支持");
    }

    /**
     * 请求 Content-Type 不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public BaseResponse<?> httpMediaTypeNotSupportedExceptionHandler(HttpMediaTypeNotSupportedException e) {
        log.warn("HttpMediaTypeNotSupportedException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请求类型不支持");
    }

    /**
     * multipart 场景缺少必须 part（例如缺少 file）
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public BaseResponse<?> missingServletRequestPartExceptionHandler(MissingServletRequestPartException e) {
        log.warn("MissingServletRequestPartException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "缺少必要的文件参数");
    }

    /**
     * 线程池任务队列已满时抛出，返回更友好的提示
     */
    @ExceptionHandler(RejectedExecutionException.class)
    public BaseResponse<?> rejectedExecutionExceptionHandler(RejectedExecutionException e) {
        log.error("RejectedExecutionException: 任务队列已满", e);
        return ResultUtils.error(ErrorCode.CHART_TASK_REJECTED);
    }

    /**
     * 处理异步链路包装异常（CompletableFuture / Future）
     * 优先透传业务异常或参数类异常，减少前端收到无意义的“系统错误”
     */
    @ExceptionHandler({CompletionException.class, ExecutionException.class})
    public BaseResponse<?> asyncWrappedExceptionHandler(Exception e) {
        BusinessException businessException = findCause(e, BusinessException.class);
        if (businessException != null) {
            log.error("Async wrapped BusinessException", e);
            return ResultUtils.error(businessException.getCode(), businessException.getMessage());
        }

        RejectedExecutionException rejectedExecutionException = findCause(e, RejectedExecutionException.class);
        if (rejectedExecutionException != null) {
            return rejectedExecutionExceptionHandler(rejectedExecutionException);
        }

        IllegalArgumentException illegalArgumentException = findCause(e, IllegalArgumentException.class);
        if (illegalArgumentException != null) {
            return illegalArgumentExceptionHandler(illegalArgumentException);
        }

        log.error("Async wrapped exception", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 从异常链中查找指定类型异常
     */
    private <T extends Throwable> T findCause(Throwable throwable, Class<T> targetType) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (targetType.isInstance(cursor)) {
                return targetType.cast(cursor);
            }
            cursor = cursor.getCause();
        }
        return null;
    }
}
