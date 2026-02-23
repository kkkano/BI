package com.yupi.springbootinit.exception;

import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
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
     * 异步接口注释说"由全局异常处理器兜底"，此处补全对应处理
     */
    @ExceptionHandler(RejectedExecutionException.class)
    public BaseResponse<?> rejectedExecutionExceptionHandler(RejectedExecutionException e) {
        log.error("RejectedExecutionException: 任务队列已满", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "当前请求繁忙，请稍后再试");
    }

    /**
     * 处理异步链路包装异常（CompletableFuture / Future）
     * 优先透传 BusinessException，其余按系统错误返回统一 BaseResponse
     */
    @ExceptionHandler({CompletionException.class, ExecutionException.class})
    public BaseResponse<?> asyncWrappedExceptionHandler(Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof BusinessException) {
            BusinessException businessException = (BusinessException) cause;
            log.error("Async wrapped BusinessException", e);
            return ResultUtils.error(businessException.getCode(), businessException.getMessage());
        }
        log.error("Async wrapped exception", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
