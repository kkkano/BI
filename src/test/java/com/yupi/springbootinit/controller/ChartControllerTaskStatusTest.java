package com.yupi.springbootinit.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.yupi.springbootinit.model.dto.chart.ChartTaskStatusBatchRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.model.vo.ChartTaskStatusBatchVO;
import com.yupi.springbootinit.model.vo.ChartTaskStatusVO;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartControllerTaskStatusTest {

    private ChartController chartController;

    @Mock
    private ChartService chartService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        chartController = new ChartController();
        ReflectionTestUtils.setField(chartController, "chartService", chartService);
        ReflectionTestUtils.setField(chartController, "userService", userService);
    }

    @Test
    void getChartTaskStatusShouldHideGeneratedContentBeforeSucceed() {
        long chartId = 1L;
        User loginUser = buildUser(100L);
        Chart chart = buildChart(chartId, loginUser.getId(), ChartStatusEnum.RUNNING.getValue());
        chart.setGenChart("{\"title\":\"demo\"}");
        chart.setGenResult("analysis");

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.getOne(any())).thenReturn(chart);

        BaseResponse<ChartTaskStatusVO> response = chartController.getChartTaskStatus(chartId, request);

        assertNotNull(response.getData());
        assertEquals(ChartStatusEnum.RUNNING.getValue(), response.getData().getStatus());
        assertNull(response.getData().getGenChart());
        assertNull(response.getData().getGenResult());
    }

    @Test
    void getChartTaskStatusShouldExposeGeneratedContentWhenSucceed() {
        long chartId = 2L;
        User loginUser = buildUser(200L);
        Chart chart = buildChart(chartId, loginUser.getId(), ChartStatusEnum.SUCCEED.getValue());
        Chart contentChart = new Chart();
        contentChart.setId(chartId);
        contentChart.setUserId(loginUser.getId());
        contentChart.setGenChart("{\"series\":[]}");
        contentChart.setGenResult("ok");

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.getOne(any())).thenReturn(chart);
        when(chartService.list(any())).thenReturn(Collections.singletonList(contentChart));

        BaseResponse<ChartTaskStatusVO> response = chartController.getChartTaskStatus(chartId, request);

        assertNotNull(response.getData());
        assertEquals(ChartStatusEnum.SUCCEED.getValue(), response.getData().getStatus());
        assertEquals(contentChart.getGenChart(), response.getData().getGenChart());
        assertEquals(contentChart.getGenResult(), response.getData().getGenResult());
    }

    @Test
    void getChartTaskStatusShouldRejectUnauthorizedWhenChartOwnerMissing() {
        long chartId = 3L;
        User loginUser = buildUser(300L);
        Chart chart = buildChart(chartId, 999L, ChartStatusEnum.RUNNING.getValue());
        chart.setUserId(null);

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.getOne(any())).thenReturn(chart);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.getChartTaskStatus(chartId, request));

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getChartTaskStatusBatchShouldReturnOwnedChartsOnlyForNormalUser() {
        User loginUser = buildUser(400L);
        Chart ownedRunningChart = buildChart(11L, loginUser.getId(), ChartStatusEnum.RUNNING.getValue());

        Chart anotherUserChart = buildChart(12L, 888L, ChartStatusEnum.SUCCEED.getValue());

        Chart ownedSucceedChart = buildChart(13L, loginUser.getId(), ChartStatusEnum.SUCCEED.getValue());
        Chart ownedSucceedChartContent = new Chart();
        ownedSucceedChartContent.setId(13L);
        ownedSucceedChartContent.setUserId(loginUser.getId());
        ownedSucceedChartContent.setGenChart("{\"title\":\"mine\"}");
        ownedSucceedChartContent.setGenResult("mine");

        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(12L, 11L, 11L, 13L, 999L));

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.list(any()))
                .thenReturn(Arrays.asList(ownedRunningChart, anotherUserChart, ownedSucceedChart))
                .thenReturn(Collections.singletonList(ownedSucceedChartContent));

        BaseResponse<List<ChartTaskStatusVO>> response = chartController.getChartTaskStatusBatch(batchRequest, request);

        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(11L, response.getData().get(0).getChartId());
        assertNull(response.getData().get(0).getGenChart());
        assertNull(response.getData().get(0).getGenResult());
        assertEquals(13L, response.getData().get(1).getChartId());
        assertEquals(ownedSucceedChartContent.getGenChart(), response.getData().get(1).getGenChart());
        assertEquals(ownedSucceedChartContent.getGenResult(), response.getData().get(1).getGenResult());
    }

    @Test
    void getChartTaskStatusBatchDetailShouldExposeUnavailableChartIds() {
        User loginUser = buildUser(450L);
        Chart ownedRunningChart = buildChart(61L, loginUser.getId(), ChartStatusEnum.RUNNING.getValue());
        Chart ownedSucceedChart = buildChart(62L, loginUser.getId(), ChartStatusEnum.SUCCEED.getValue());
        Chart anotherUserChart = buildChart(64L, 9000L, ChartStatusEnum.SUCCEED.getValue());

        Chart ownedSucceedChartContent = new Chart();
        ownedSucceedChartContent.setId(62L);
        ownedSucceedChartContent.setUserId(loginUser.getId());
        ownedSucceedChartContent.setGenChart("{\"series\":[1]}");
        ownedSucceedChartContent.setGenResult("done");

        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(61L, 62L, 63L, 64L));

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.list(any()))
                .thenReturn(Arrays.asList(ownedRunningChart, ownedSucceedChart, anotherUserChart))
                .thenReturn(Collections.singletonList(ownedSucceedChartContent));

        BaseResponse<ChartTaskStatusBatchVO> response = chartController.getChartTaskStatusBatchDetail(batchRequest, request);

        assertNotNull(response.getData());
        assertEquals(4, response.getData().getRequestedCount());
        assertEquals(2, response.getData().getReturnedCount());
        assertEquals(Arrays.asList(63L, 64L), response.getData().getUnavailableChartIds());
        assertEquals(2, response.getData().getTaskStatusList().size());
        assertEquals(61L, response.getData().getTaskStatusList().get(0).getChartId());
        assertEquals(62L, response.getData().getTaskStatusList().get(1).getChartId());
    }

    @Test
    void getChartTaskStatusBatchShouldReturnAllChartsForAdmin() {
        User loginUser = buildUser(500L);
        Chart chartA = buildChart(21L, 1000L, ChartStatusEnum.RUNNING.getValue());
        Chart chartB = buildChart(22L, 2000L, ChartStatusEnum.SUCCEED.getValue());
        Chart chartBContent = new Chart();
        chartBContent.setId(22L);
        chartBContent.setUserId(2000L);
        chartBContent.setGenChart("{\"title\":\"admin\"}");
        chartBContent.setGenResult("admin");

        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(21L, 22L));

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(true);
        when(chartService.list(any()))
                .thenReturn(Arrays.asList(chartA, chartB))
                .thenReturn(Collections.singletonList(chartBContent));

        BaseResponse<List<ChartTaskStatusVO>> response = chartController.getChartTaskStatusBatch(batchRequest, request);

        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(21L, response.getData().get(0).getChartId());
        assertEquals(22L, response.getData().get(1).getChartId());
        assertEquals(chartBContent.getGenChart(), response.getData().get(1).getGenChart());
    }

    @Test
    void getChartTaskStatusBatchShouldAppendUserFilterForNormalUserQuery() {
        User loginUser = buildUser(700L);
        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(41L, 42L));

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.list(any())).thenReturn(Collections.emptyList());

        chartController.getChartTaskStatusBatch(batchRequest, request);

        ArgumentCaptor<QueryWrapper> queryWrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(chartService).list(queryWrapperCaptor.capture());
        String sqlSegment = queryWrapperCaptor.getValue().getCustomSqlSegment();
        assertTrue(sqlSegment.contains("userId"));
    }

    @Test
    void getChartTaskStatusBatchShouldNotAppendUserFilterForAdminQuery() {
        User loginUser = buildUser(800L);
        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(51L, 52L));

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(true);
        when(chartService.list(any())).thenReturn(Collections.emptyList());

        chartController.getChartTaskStatusBatch(batchRequest, request);

        ArgumentCaptor<QueryWrapper> queryWrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(chartService).list(queryWrapperCaptor.capture());
        String sqlSegment = queryWrapperCaptor.getValue().getCustomSqlSegment();
        assertFalse(sqlSegment.contains("userId ="));
    }

    @Test
    void getChartTaskStatusBatchShouldRejectNullChartIds() {
        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.getChartTaskStatusBatch(batchRequest, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getChartTaskStatusBatchShouldRejectInvalidChartId() {
        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(41L, 0L, 42L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.getChartTaskStatusBatch(batchRequest, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("图表 id 非法", exception.getMessage());
    }

    @Test
    void getChartTaskStatusBatchShouldReturnEmptyWhenQueryResultIsNull() {
        User loginUser = buildUser(600L);
        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(31L, 32L));

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.list(any())).thenReturn(null);

        BaseResponse<List<ChartTaskStatusVO>> response = chartController.getChartTaskStatusBatch(batchRequest, request);

        assertNotNull(response.getData());
        assertEquals(0, response.getData().size());
    }

    @Test
    void getChartTaskStatusBatchShouldRejectWhenRequestExceedsMaxSize() {
        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(
                1L, 2L, 3L, 4L, 5L,
                6L, 7L, 8L, 9L, 10L,
                11L, 12L, 13L, 14L, 15L,
                16L, 17L, 18L, 19L, 20L, 21L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.getChartTaskStatusBatch(batchRequest, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("单次最多查询 20 个图表", exception.getMessage());
    }

    @Test
    void getChartTaskStatusBatchShouldRejectWhenDuplicateRequestExceedsMaxSize() {
        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Collections.nCopies(21, 1L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.getChartTaskStatusBatch(batchRequest, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("单次最多查询 20 个图表", exception.getMessage());
    }

    @Test
    void listChartByPageShouldRequireAdminRole() throws NoSuchMethodException {
        Method method = ChartController.class.getMethod("listChartByPage", ChartQueryRequest.class, HttpServletRequest.class);

        AuthCheck authCheck = method.getAnnotation(AuthCheck.class);

        assertNotNull(authCheck);
        assertEquals(UserConstant.ADMIN_ROLE, authCheck.mustRole());
    }

    private User buildUser(long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private Chart buildChart(long chartId, long userId, String status) {
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setUserId(userId);
        chart.setStatus(status);
        return chart;
    }
}
