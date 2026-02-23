package com.yupi.springbootinit.model.dto.chart;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void chartAddRequestShouldRejectBlankGoal() {
        ChartAddRequest request = new ChartAddRequest();
        request.setGoal("   ");

        Set<ConstraintViolation<ChartAddRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "分析目标不能为空".equals(v.getMessage())));
    }

    @Test
    void chartEditRequestShouldRejectNonPositiveId() {
        ChartEditRequest request = new ChartEditRequest();
        request.setId(0L);

        Set<ConstraintViolation<ChartEditRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "图表 id 非法".equals(v.getMessage())));
    }

    @Test
    void chartQueryRequestShouldRejectTooLongStatus() {
        ChartQueryRequest request = new ChartQueryRequest();
        request.setStatus(repeat('a', 33));

        Set<ConstraintViolation<ChartQueryRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "任务状态字段过长".equals(v.getMessage())));
    }

    @Test
    void chartUpdateRequestShouldRejectTooLongExecMessage() {
        ChartUpdateRequest request = new ChartUpdateRequest();
        request.setId(1L);
        request.setExecMessage(repeat('b', 2001));

        Set<ConstraintViolation<ChartUpdateRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "执行信息过长".equals(v.getMessage())));
    }

    @Test
    void chartUpdateRequestShouldPassWhenWithinBoundary() {
        ChartUpdateRequest request = new ChartUpdateRequest();
        request.setId(1L);
        request.setName(repeat('n', 128));
        request.setChartType(repeat('t', 128));
        request.setStatus(repeat('s', 32));
        request.setExecMessage(repeat('m', 2000));

        Set<ConstraintViolation<ChartUpdateRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
