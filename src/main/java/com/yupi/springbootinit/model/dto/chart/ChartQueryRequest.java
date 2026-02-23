package com.yupi.springbootinit.model.dto.chart;

import com.yupi.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 查询请求
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    /** 图表 id */
    @Positive(message = "图表 id 非法")
    private Long id;

    /** 图表名称 */
    @Size(max = 128, message = "图表名称过长")
    private String name;

    /** 分析目标 */
    @Size(max = 2000, message = "分析目标过长")
    private String goal;

    /** 图表类型 */
    @Size(max = 128, message = "图表类型过长")
    private String chartType;

    /** 任务状态：wait / running / succeed / failed */
    @Size(max = 32, message = "任务状态字段过长")
    private String status;

    /** 创建用户 id */
    @Positive(message = "用户 id 非法")
    private Long userId;

    private static final long serialVersionUID = 1L;
}
