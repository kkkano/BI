package com.yupi.springbootinit.model.dto.chart;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 更新请求（管理员）
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
@Data
public class ChartUpdateRequest implements Serializable {

    /** 图表 id */
    @NotNull(message = "图表 id 不能为空")
    @Positive(message = "图表 id 非法")
    private Long id;

    /** 图表名称 */
    @Size(max = 128, message = "图表名称过长")
    private String name;

    /** 分析目标 */
    @Size(max = 2000, message = "分析目标过长")
    private String goal;

    /** 图表数据 */
    private String chartData;

    /** 图表类型 */
    @Size(max = 128, message = "图表类型过长")
    private String chartType;

    /** 生成图表配置 */
    private String genChart;

    /** 生成分析结论 */
    private String genResult;

    /** 任务状态：wait / running / succeed / failed */
    @Size(max = 32, message = "任务状态字段过长")
    private String status;

    /** 任务执行信息 */
    @Size(max = 2000, message = "执行信息过长")
    private String execMessage;

    private static final long serialVersionUID = 1L;
}
