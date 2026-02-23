package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.model.dto.chart.ChartQueryRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
