package com.yupi.springbootinit.model.dto.chart;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 创建请求
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
@Data
public class ChartAddRequest implements Serializable {

    /** 图表名称 */
    @Size(max = 128, message = "图表名称过长")
    private String name;

    /** 分析目标 */
    @NotBlank(message = "分析目标不能为空")
    @Size(max = 2000, message = "分析目标过长")
    private String goal;

    /** 图表数据 */
    private String chartData;

    /** 图表类型 */
    @Size(max = 128, message = "图表类型过长")
    private String chartType;

    private static final long serialVersionUID = 1L;
}
