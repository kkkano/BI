package com.yupi.springbootinit.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void shouldRejectNullId() {
        DeleteRequest request = new DeleteRequest();

        Set<ConstraintViolation<DeleteRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "id 不能为空".equals(v.getMessage())));
    }

    @Test
    void shouldRejectNonPositiveId() {
        DeleteRequest request = new DeleteRequest();
        request.setId(0L);

        Set<ConstraintViolation<DeleteRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "id 非法".equals(v.getMessage())));
    }

    @Test
    void shouldPassWhenIdIsPositive() {
        DeleteRequest request = new DeleteRequest();
        request.setId(1L);

        Set<ConstraintViolation<DeleteRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }
}
