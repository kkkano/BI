package com.yupi.springbootinit.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * SQL 工具
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
public class SqlUtils {

    /** 仅允许字母、数字和下划线，防止 sort 字段 SQL 注入 */
    private static final Pattern SAFE_SORT_FIELD_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    private static final int MAX_SORT_FIELD_LENGTH = 64;

    /**
     * 校验排序字段是否合法（防止 SQL 注入）
     *
     * @param sortField 排序字段
     * @return 是否合法
     */
    public static boolean validSortField(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return false;
        }
        if (sortField.length() > MAX_SORT_FIELD_LENGTH) {
            return false;
        }
        return SAFE_SORT_FIELD_PATTERN.matcher(sortField).matches();
    }
}
