package com.yupi.springbootinit.model.dto.chart;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 文件上传请求
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
@Data
public class GenChartByAiRequest implements Serializable {

    /**
     * 图表名称（可选）
     */
    @Size(max = 100, message = "名称过长")
    private String name;

    /**
     * 分析目标
     */
    @NotBlank(message = "目标为空")
    @Size(max = 1000, message = "目标过长")
    private String goal;

    /**
     * 图表类型（可选）
     */
    @Size(max = 100, message = "图表类型过长")
    private String chartType;


    private static final long serialVersionUID = 1L;
}
