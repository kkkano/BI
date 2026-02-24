package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.dto.chart.ChartEditRequest;
import com.yupi.springbootinit.model.dto.chart.ChartUpdateRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartControllerCrudErrorHandlingTest {

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
    void deleteChartShouldThrowOperationErrorWhenRemoveFailed() {
        long chartId = 101L;
        User loginUser = buildUser(1L);
        Chart oldChart = buildChart(chartId, loginUser.getId());

        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.setId(chartId);

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(request)).thenReturn(false);
        when(chartService.getById(chartId)).thenReturn(oldChart);
        when(chartService.removeById(chartId)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.deleteChart(deleteRequest, request));

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("图表删除失败", exception.getMessage());
    }

    @Test
    void updateChartShouldThrowOperationErrorWhenUpdateFailed() {
        long chartId = 102L;
        Chart oldChart = buildChart(chartId, 2L);

        ChartUpdateRequest updateRequest = new ChartUpdateRequest();
        updateRequest.setId(chartId);

        when(chartService.getById(chartId)).thenReturn(oldChart);
        when(chartService.updateById(any(Chart.class))).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.updateChart(updateRequest));

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("图表更新失败", exception.getMessage());
    }

    @Test
    void editChartShouldThrowOperationErrorWhenUpdateFailed() {
        long chartId = 103L;
        User loginUser = buildUser(3L);
        Chart oldChart = buildChart(chartId, loginUser.getId());

        ChartEditRequest editRequest = new ChartEditRequest();
        editRequest.setId(chartId);

        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.isAdmin(loginUser)).thenReturn(false);
        when(chartService.getById(chartId)).thenReturn(oldChart);
        when(chartService.updateById(any(Chart.class))).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chartController.editChart(editRequest, request));

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("图表编辑失败", exception.getMessage());
    }

    private User buildUser(long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private Chart buildChart(long chartId, long userId) {
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setUserId(userId);
        return chart;
    }
}
