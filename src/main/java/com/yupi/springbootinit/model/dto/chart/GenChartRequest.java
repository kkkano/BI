package com.yupi.springbootinit.model.dto.chart;

import lombok.Data;

/**
 * 生成图表服务层请求
 */
@Data
public class GenChartRequest {

    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 上传文件解析后的 CSV 数据
     */
    private String csvData;
}
