package com.yupi.springbootinit.controller;

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
import com.yupi.springbootinit.model.vo.ChartTaskStatusVO;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
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
        when(chartService.getById(chartId)).thenReturn(chart);

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
        chart.setGenChart("{\"series\":[]}");
        chart.setGenResult("ok");

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.getById(chartId)).thenReturn(chart);

        BaseResponse<ChartTaskStatusVO> response = chartController.getChartTaskStatus(chartId, request);

        assertNotNull(response.getData());
        assertEquals(ChartStatusEnum.SUCCEED.getValue(), response.getData().getStatus());
        assertEquals(chart.getGenChart(), response.getData().getGenChart());
        assertEquals(chart.getGenResult(), response.getData().getGenResult());
    }

    @Test
    void getChartTaskStatusShouldRejectUnauthorizedWhenChartOwnerMissing() {
        long chartId = 3L;
        User loginUser = buildUser(300L);
        Chart chart = buildChart(chartId, 999L, ChartStatusEnum.RUNNING.getValue());
        chart.setUserId(null);

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.getById(chartId)).thenReturn(chart);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.getChartTaskStatus(chartId, request));

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getChartTaskStatusBatchShouldReturnOwnedChartsOnlyForNormalUser() {
        User loginUser = buildUser(400L);
        Chart ownedRunningChart = buildChart(11L, loginUser.getId(), ChartStatusEnum.RUNNING.getValue());
        ownedRunningChart.setGenChart("{\"title\":\"hidden\"}");
        ownedRunningChart.setGenResult("hidden");

        Chart anotherUserChart = buildChart(12L, 888L, ChartStatusEnum.SUCCEED.getValue());
        anotherUserChart.setGenChart("{\"title\":\"other\"}");
        anotherUserChart.setGenResult("other");

        Chart ownedSucceedChart = buildChart(13L, loginUser.getId(), ChartStatusEnum.SUCCEED.getValue());
        ownedSucceedChart.setGenChart("{\"title\":\"mine\"}");
        ownedSucceedChart.setGenResult("mine");

        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(12L, 11L, 11L, 13L, 999L));

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.listByIds(anyCollection()))
                .thenReturn(Arrays.asList(ownedRunningChart, anotherUserChart, ownedSucceedChart));

        BaseResponse<List<ChartTaskStatusVO>> response = chartController.getChartTaskStatusBatch(batchRequest, request);

        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(11L, response.getData().get(0).getChartId());
        assertNull(response.getData().get(0).getGenChart());
        assertNull(response.getData().get(0).getGenResult());
        assertEquals(13L, response.getData().get(1).getChartId());
        assertEquals(ownedSucceedChart.getGenChart(), response.getData().get(1).getGenChart());
        assertEquals(ownedSucceedChart.getGenResult(), response.getData().get(1).getGenResult());
    }

    @Test
    void getChartTaskStatusBatchShouldReturnAllChartsForAdmin() {
        User loginUser = buildUser(500L);
        Chart chartA = buildChart(21L, 1000L, ChartStatusEnum.RUNNING.getValue());
        Chart chartB = buildChart(22L, 2000L, ChartStatusEnum.SUCCEED.getValue());
        chartB.setGenChart("{\"title\":\"admin\"}");
        chartB.setGenResult("admin");

        ChartTaskStatusBatchRequest batchRequest = new ChartTaskStatusBatchRequest();
        batchRequest.setChartIds(Arrays.asList(21L, 22L));

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(true);
        when(chartService.listByIds(anyCollection())).thenReturn(Arrays.asList(chartA, chartB));

        BaseResponse<List<ChartTaskStatusVO>> response = chartController.getChartTaskStatusBatch(batchRequest, request);

        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(21L, response.getData().get(0).getChartId());
        assertEquals(22L, response.getData().get(1).getChartId());
        assertEquals(chartB.getGenChart(), response.getData().get(1).getGenChart());
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
