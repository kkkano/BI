package com.yupi.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.model.vo.ChartTaskStatusVO;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
@Validated
@Api(tags = "图表管理")
public class ChartController {

    /** 支持上传的文件后缀白名单（统一维护） */
    private static final List<String> VALID_FILE_SUFFIXES = Arrays.asList("xlsx", "xls", "csv");

    /** 上传文件大小上限：1 MB */
    private static final long MAX_FILE_SIZE = 1024 * 1024L;

    /** 批量任务状态查询上限 */
    private static final int MAX_BATCH_TASK_STATUS_SIZE = 20;

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;


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
    @ApiOperation(value = "创建图表")
    public BaseResponse<Long> addChart(@RequestBody @Valid ChartAddRequest chartAddRequest, HttpServletRequest request) {
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
    @ApiOperation(value = "删除图表")
    public BaseResponse<Boolean> deleteChart(@RequestBody @Valid DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!Objects.equals(oldChart.getUserId(), user.getId()) && !userService.isAdmin(request)) {
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
    @ApiOperation(value = "更新图表（管理员）")
    public BaseResponse<Boolean> updateChart(@RequestBody @Valid ChartUpdateRequest chartUpdateRequest) {
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
    @ApiOperation(value = "根据 id 获取图表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "图表 id", required = true, dataType = "long", paramType = "query")
    })
    public BaseResponse<Chart> getChartById(@RequestParam("id") @Min(value = 1, message = "图表 id 非法") long id,
                                            HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (!Objects.equals(chart.getUserId(), loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 获取图表任务状态（轻量轮询接口）
     * 仅返回任务状态相关字段，前端轮询使用
     */
    @GetMapping("/task/status")
    @ApiOperation(value = "获取图表任务状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chartId", value = "图表 id", required = true, dataType = "long", paramType = "query")
    })
    public BaseResponse<ChartTaskStatusVO> getChartTaskStatus(@RequestParam("chartId") @Min(value = 1, message = "图表 id 非法") long chartId,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(chartId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<Chart> queryWrapper = buildTaskStatusQueryWrapper();
        queryWrapper.eq("id", chartId);
        Chart chart = chartService.getOne(queryWrapper);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可看任务详情
        if (!Objects.equals(chart.getUserId(), loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(buildTaskStatusVO(chart));
    }

    /**
     * 批量获取图表任务状态（轻量轮询接口）
     * 仅返回任务状态相关字段，减少多任务并发轮询时的请求开销
     */
    @PostMapping("/task/status/batch")
    @ApiOperation(value = "批量获取图表任务状态")
    public BaseResponse<List<ChartTaskStatusVO>> getChartTaskStatusBatch(
            @RequestBody @Valid ChartTaskStatusBatchRequest batchRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(batchRequest == null, ErrorCode.PARAMS_ERROR);
        List<Long> chartIds = batchRequest.getChartIds();
        ThrowUtils.throwIf(chartIds == null || chartIds.isEmpty(), ErrorCode.PARAMS_ERROR,
                "图表 id 列表不能为空");
        ThrowUtils.throwIf(chartIds.size() > MAX_BATCH_TASK_STATUS_SIZE, ErrorCode.PARAMS_ERROR,
                "单次最多查询 20 个图表");
        for (Long chartId : chartIds) {
            ThrowUtils.throwIf(chartId == null || chartId <= 0, ErrorCode.PARAMS_ERROR,
                    "图表 id 非法");
        }
        Set<Long> chartIdSet = new LinkedHashSet<>(chartIds);

        User loginUser = userService.getLoginUser(request);
        boolean isAdmin = userService.isAdmin(request);

        QueryWrapper<Chart> queryWrapper = buildTaskStatusQueryWrapper();
        queryWrapper.in("id", chartIdSet);
        if (!isAdmin) {
            queryWrapper.eq("userId", loginUser.getId());
        }
        List<Chart> charts = chartService.list(queryWrapper);
        if (charts == null || charts.isEmpty()) {
            return ResultUtils.success(new ArrayList<>());
        }
        Map<Long, Chart> chartMap = new HashMap<>(charts.size());
        for (Chart chart : charts) {
            chartMap.put(chart.getId(), chart);
        }

        List<ChartTaskStatusVO> responseList = new ArrayList<>();
        for (Long chartId : chartIdSet) {
            Chart chart = chartMap.get(chartId);
            if (chart == null) {
                continue;
            }
            if (!isAdmin && !Objects.equals(chart.getUserId(), loginUser.getId())) {
                continue;
            }
            responseList.add(buildTaskStatusVO(chart));
        }
        return ResultUtils.success(responseList);
    }

    /**
     * 分页获取图表列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "分页获取图表列表（管理员）")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody @Valid ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(chartQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        validatePageParams(current, size);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户的图表列表
     */
    @PostMapping("/my/list/page")
    @ApiOperation(value = "分页获取当前用户图表列表")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody @Valid ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        validatePageParams(current, size);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑图表（用户）
     */
    @PostMapping("/edit")
    @ApiOperation(value = "编辑图表（用户）")
    public BaseResponse<Boolean> editChart(@RequestBody @Valid ChartEditRequest chartEditRequest,
                                           HttpServletRequest request) {
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
        if (!Objects.equals(oldChart.getUserId(), loginUser.getId()) && !userService.isAdmin(loginUser)) {
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
    @ApiOperation(value = "智能分析（同步）")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 @Valid GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        validateGenChartRequest(genChartByAiRequest);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        validateUploadFile(multipartFile);

        User loginUser = userService.getLoginUser(request);
        checkPointsAndRateLimit(loginUser);

        String csvData = parseUploadFileToCsv(multipartFile);
        GenChartRequest genChartRequest = new GenChartRequest();
        genChartRequest.setName(name);
        genChartRequest.setGoal(goal);
        genChartRequest.setChartType(chartType);
        genChartRequest.setCsvData(csvData);

        BiResponse biResponse = chartService.generateChartSync(genChartRequest, loginUser);
        if (biResponse == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成或结果更新失败");
        }
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步线程池）
     * 立即返回 chartId，后台异步执行 AI 分析，前端轮询 /chart/task/status 获取结果
     */
    @PostMapping("/gen/async")
    @ApiOperation(value = "智能分析（异步线程池）")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      @Valid GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        validateGenChartRequest(genChartByAiRequest);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        validateUploadFile(multipartFile);

        User loginUser = userService.getLoginUser(request);
        checkPointsAndRateLimit(loginUser);
        if (isAsyncExecutorSaturated()) {
            log.warn("线程池繁忙，拒绝创建异步图表任务 userId={}, activeCount={}, maxPoolSize={}, queueRemainingCapacity={}",
                    loginUser.getId(),
                    threadPoolExecutor.getActiveCount(),
                    threadPoolExecutor.getMaximumPoolSize(),
                    getExecutorQueueRemainingCapacity());
            throw new BusinessException(ErrorCode.CHART_TASK_REJECTED);
        }

        String csvData = parseUploadFileToCsv(multipartFile);
        String userInput = chartService.buildUserInput(goal, chartType, csvData);

        GenChartRequest genChartRequest = new GenChartRequest();
        genChartRequest.setName(name);
        genChartRequest.setGoal(goal);
        genChartRequest.setChartType(chartType);
        genChartRequest.setCsvData(csvData);

        BiResponse biResponse = chartService.createAsyncThreadTask(genChartRequest, loginUser);
        long chartId = biResponse.getChartId();
        log.info("异步图表任务已创建 chartId={}, userId={}, status={}",
                chartId, loginUser.getId(), ChartStatusEnum.WAIT.getValue());

        // 异步执行 AI 分析
        // 当线程池满时，降级把任务状态置为失败，避免任务长期停留在 wait
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("异步图表任务开始执行 chartId={}, userId={}, status={}",
                            chartId, loginUser.getId(), ChartStatusEnum.RUNNING.getValue());
                    chartService.executeChartGeneration(chartId, userInput);
                } catch (Exception e) {
                    log.error("异步生成图表异常，chartId={}, userId={}, status={}",
                            chartId, loginUser.getId(), ChartStatusEnum.FAILED.getValue(), e);
                    chartService.handleChartUpdateError(chartId,
                            ErrorCode.CHART_TASK_EXECUTE_EXCEPTION.getCode() + ": " +
                                    ErrorCode.CHART_TASK_EXECUTE_EXCEPTION.getMessage() + " - " + e.getMessage());
                }
            }, threadPoolExecutor);
        } catch (RejectedExecutionException e) {
            log.error("线程池繁忙，异步任务提交失败，chartId={}, userId={}, status={}",
                    chartId, loginUser.getId(), ChartStatusEnum.FAILED.getValue(), e);
            chartService.handleChartUpdateError(chartId,
                    ErrorCode.CHART_TASK_REJECTED.getCode() + ": " + ErrorCode.CHART_TASK_REJECTED.getMessage());
            throw new BusinessException(ErrorCode.CHART_TASK_REJECTED);
        }

        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步消息队列）
     * 立即返回 chartId，通过 RabbitMQ 异步处理 AI 分析
     */
    @PostMapping("/gen/async/mq")
    @ApiOperation(value = "智能分析（异步消息队列）")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                        @Valid GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        validateGenChartRequest(genChartByAiRequest);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        validateUploadFile(multipartFile);

        User loginUser = userService.getLoginUser(request);
        checkPointsAndRateLimit(loginUser);

        String csvData = parseUploadFileToCsv(multipartFile);

        GenChartRequest genChartRequest = new GenChartRequest();
        genChartRequest.setName(name);
        genChartRequest.setGoal(goal);
        genChartRequest.setChartType(chartType);
        genChartRequest.setCsvData(csvData);

        BiResponse biResponse = chartService.createAsyncMqTask(genChartRequest, loginUser);
        long chartId = biResponse.getChartId();
        log.info("MQ异步图表任务已创建 chartId={}, userId={}, status={}",
                chartId, loginUser.getId(), ChartStatusEnum.WAIT.getValue());

        // 发送消息到 MQ，由消费者异步处理
        try {
            biMessageProducer.sendMessage(String.valueOf(chartId));
        } catch (Exception e) {
            log.error("MQ异步图表任务投递失败，chartId={}, userId={}, status={}",
                    chartId, loginUser.getId(), ChartStatusEnum.FAILED.getValue(), e);
            chartService.handleChartUpdateError(chartId,
                    ErrorCode.CHART_TASK_MESSAGE_SEND_FAILED.getCode() + ": "
                            + ErrorCode.CHART_TASK_MESSAGE_SEND_FAILED.getMessage());
            throw new BusinessException(ErrorCode.CHART_TASK_MESSAGE_SEND_FAILED);
        }
        return ResultUtils.success(biResponse);
    }

    // region 私有工具方法

    /**
     * 构建任务状态响应，任务未成功时不返回生成内容
     */
    private ChartTaskStatusVO buildTaskStatusVO(Chart chart) {
        ChartTaskStatusVO vo = new ChartTaskStatusVO();
        vo.setChartId(chart.getId());
        vo.setName(chart.getName());
        vo.setGoal(chart.getGoal());
        vo.setChartType(chart.getChartType());
        vo.setStatus(chart.getStatus());
        vo.setExecMessage(chart.getExecMessage());
        if (ChartStatusEnum.SUCCEED.getValue().equals(chart.getStatus())) {
            vo.setGenChart(chart.getGenChart());
            vo.setGenResult(chart.getGenResult());
        }
        vo.setCreateTime(chart.getCreateTime());
        vo.setUpdateTime(chart.getUpdateTime());
        return vo;
    }

    /**
     * 任务状态查询通用字段（避免读取 chartData 大字段）
     */
    private QueryWrapper<Chart> buildTaskStatusQueryWrapper() {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "name", "goal", "chartType", "status", "execMessage",
                "genChart", "genResult", "userId", "createTime", "updateTime");
        queryWrapper.eq("isDelete", false);
        return queryWrapper;
    }

    /**
     * 校验 gen 接口请求参数，避免请求体缺失时出现空指针
     */
    private void validateGenChartRequest(GenChartByAiRequest genChartByAiRequest) {
        ThrowUtils.throwIf(genChartByAiRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(StringUtils.isBlank(genChartByAiRequest.getGoal()),
                ErrorCode.PARAMS_ERROR, "目标为空");
    }

    /**
     * 校验 gen 接口通用参数（文件大小与后缀）
     */
    private void validateUploadFile(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null || multipartFile.isEmpty(),
                ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        ThrowUtils.throwIf(multipartFile.getSize() > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        String originalFilename = multipartFile.getOriginalFilename();
        ThrowUtils.throwIf(originalFilename == null, ErrorCode.PARAMS_ERROR, "文件名非法");
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(suffix == null || !VALID_FILE_SUFFIXES.contains(suffix.toLowerCase(Locale.ROOT)),
                ErrorCode.PARAMS_ERROR, "文件后缀非法，仅支持 xlsx / xls / csv");
    }

    /**
     * 将上传文件转换为 csv，并在解析失败时给出明确提示
     */
    private String parseUploadFileToCsv(MultipartFile multipartFile) {
        String csvData = ExcelUtils.fileToCsv(multipartFile);
        ThrowUtils.throwIf(StringUtils.isBlank(csvData), ErrorCode.PARAMS_ERROR,
                "文件内容为空或解析失败，请检查文件内容与格式");
        return csvData;
    }

    /**
     * 校验分页参数并限制 pageSize，避免异常值导致全表扫描
     */
    private void validatePageParams(long current, long size) {
        ThrowUtils.throwIf(current <= 0 || size <= 0, ErrorCode.PARAMS_ERROR, "分页参数非法");
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
    }

    /**
     * 判断异步执行器是否已饱和（线程池线程占满且队列无剩余容量）
     */
    private boolean isAsyncExecutorSaturated() {
        if (threadPoolExecutor == null) {
            return false;
        }
        return threadPoolExecutor.getActiveCount() >= threadPoolExecutor.getMaximumPoolSize()
                && getExecutorQueueRemainingCapacity() <= 0;
    }

    private int getExecutorQueueRemainingCapacity() {
        if (threadPoolExecutor == null) {
            return Integer.MAX_VALUE;
        }
        BlockingQueue<Runnable> queue = threadPoolExecutor.getQueue();
        if (queue == null) {
            return Integer.MAX_VALUE;
        }
        return queue.remainingCapacity();
    }

    /**
     * 校验积分并执行限流
     */
    private void checkPointsAndRateLimit(User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Integer points = loginUser.getPoints();
        if (points == null || points < 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "积分不足，无法使用此服务");
        }
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
    }

    // endregion
}
