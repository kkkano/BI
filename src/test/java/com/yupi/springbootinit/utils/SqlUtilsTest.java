package com.yupi.springbootinit.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlUtilsTest {

    @Test
    void validSortFieldShouldAllowSafeFieldNames() {
        assertTrue(SqlUtils.validSortField("createTime"));
        assertTrue(SqlUtils.validSortField("user_id"));
        assertTrue(SqlUtils.validSortField("field123"));
    }

    @Test
    void validSortFieldShouldRejectBlankAndTooLongInput() {
        assertFalse(SqlUtils.validSortField(null));
        assertFalse(SqlUtils.validSortField(""));
        assertFalse(SqlUtils.validSortField("   "));
        assertFalse(SqlUtils.validSortField("a".repeat(65)));
    }

    @Test
    void validSortFieldShouldRejectUnsafeCharacters() {
        assertFalse(SqlUtils.validSortField("createTime desc"));
        assertFalse(SqlUtils.validSortField("createTime;drop"));
        assertFalse(SqlUtils.validSortField("createTime--"));
        assertFalse(SqlUtils.validSortField("createTime/*x*/"));
        assertFalse(SqlUtils.validSortField("createTime,id"));
        assertFalse(SqlUtils.validSortField("createTime=1"));
    }
}
