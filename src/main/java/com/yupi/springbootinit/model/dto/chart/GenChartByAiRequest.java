package com.yupi.springbootinit.model.dto.chart;

import lombok.Data;

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
     * 业务
     */
    private String name;
    private String goal;
    private String chartType;


    private static final long serialVersionUID = 1L;
}