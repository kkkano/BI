package com.yupi.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.model.vo.ChartTaskStatusVO;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionException;

/**
 * 图表接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    /** 支持上传的文件后缀白名单（统一维护） */
    private static final List<String> VALID_FILE_SUFFIXES = Arrays.asList("xlsx", "xls");

    /** 上传文件大小上限：1 MB */
    private static final long MAX_FILE_SIZE = 1024 * 1024L;

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;


    // region 增删改查

    /**
     * 创建图表
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除图表
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新图表（仅管理员）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取图表
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 获取图表任务状态（轻量轮询接口）
     * 仅返回任务状态相关字段，前端轮询使用
     */
    @GetMapping("/task/status")
    @ApiOperation(value = "获取图表任务状态")
    public BaseResponse<ChartTaskStatusVO> getChartTaskStatus(long chartId, HttpServletRequest request) {
        ThrowUtils.throwIf(chartId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Chart chart = chartService.getById(chartId);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可看任务详情
        if (!chart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        ChartTaskStatusVO vo = new ChartTaskStatusVO();
        vo.setChartId(chart.getId());
        vo.setName(chart.getName());
        vo.setGoal(chart.getGoal());
        vo.setChartType(chart.getChartType());
        vo.setStatus(chart.getStatus());
        vo.setExecMessage(chart.getExecMessage());
        vo.setGenChart(chart.getGenChart());
        vo.setGenResult(chart.getGenResult());
        return ResultUtils.success(vo);
    }

    /**
     * 分页获取图表列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户的图表列表
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑图表（用户）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 智能分析（同步）
     * 直接调用 AI，阻塞等待结果
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 @Valid GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        validateUploadFile(multipartFile);

        User loginUser = userService.getLoginUser(request);
        checkPointsAndRateLimit(loginUser);

        String csvData = ExcelUtils.excelToCsv(multipartFile);
        String userInput = chartService.buildUserInput(goal, chartType, csvData);

        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, userInput);
        String[] parsedResult = chartService.parseAiResult(result);
        if (parsedResult == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String genChart = parsedResult[0];
        String genResult = parsedResult[1];

        Chart chart = new Chart();
        chart.setStatus(ChartStatusEnum.SUCCEED.getValue());
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        // 图表保存与积分扣减同事务执行，保证一致性
        chartService.saveChartAndDeductPoint(chart, loginUser);

        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步线程池）
     * 立即返回 chartId，后台异步执行 AI 分析，前端轮询 /chart/task/status 获取结果
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      @Valid GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        validateUploadFile(multipartFile);

        User loginUser = userService.getLoginUser(request);
        checkPointsAndRateLimit(loginUser);

        String csvData = ExcelUtils.excelToCsv(multipartFile);
        String userInput = chartService.buildUserInput(goal, chartType, csvData);

        // 先入库，状态设为等待
        Chart chart = chartService.buildWaitChart(name, goal, chartType, csvData, loginUser.getId());
        chartService.saveWaitChart(chart);

        // 异步执行 AI 分析
        // 当线程池满时，降级把任务状态置为失败，避免任务长期停留在 wait
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    chartService.executeChartGeneration(chart.getId(), userInput);
                } catch (Exception e) {
                    log.error("异步生成图表异常，chartId={}", chart.getId(), e);
                    chartService.handleChartUpdateError(chart.getId(), "图表生成异常：" + e.getMessage());
                }
            }, threadPoolExecutor);
        } catch (RejectedExecutionException e) {
            log.error("线程池繁忙，异步任务提交失败，chartId={}", chart.getId(), e);
            chartService.handleChartUpdateError(chart.getId(), "系统繁忙，请稍后重试");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "当前系统繁忙，请稍后重试");
        }

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步消息队列）
     * 立即返回 chartId，通过 RabbitMQ 异步处理 AI 分析
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                        @Valid GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        validateUploadFile(multipartFile);

        User loginUser = userService.getLoginUser(request);
        checkPointsAndRateLimit(loginUser);

        String csvData = ExcelUtils.excelToCsv(multipartFile);
        String userInput = chartService.buildUserInput(goal, chartType, csvData);

        // 先入库，状态设为等待
        Chart chart = chartService.buildWaitChart(name, goal, chartType, csvData, loginUser.getId());
        chartService.saveWaitChart(chart);

        // 发送消息到 MQ，由消费者异步处理
        long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);
        return ResultUtils.success(biResponse);
    }

    // region 私有工具方法

    /**
     * 校验 gen 接口通用参数（文件大小与后缀）
     */
    private void validateUploadFile(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile.getSize() > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!VALID_FILE_SUFFIXES.contains(suffix.toLowerCase(Locale.ROOT)),
                ErrorCode.PARAMS_ERROR, "文件后缀非法");
    }

    /**
     * 校验积分并执行限流
     */
    private void checkPointsAndRateLimit(User loginUser) {
        if (loginUser.getPoints() < 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "积分不足，无法使用此服务");
        }
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
    }

    // endregion
}
