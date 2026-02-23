package com.yupi.springbootinit.model.dto.chart;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 批量查询图表任务状态请求
 */
@Data
public class ChartTaskStatusBatchRequest implements Serializable {

    @NotEmpty(message = "图表 id 列表不能为空")
    @Size(max = 20, message = "单次最多查询 20 个图表")
    private List<@Min(value = 1, message = "图表 id 非法") Long> chartIds;

    private static final long serialVersionUID = 1L;
}
