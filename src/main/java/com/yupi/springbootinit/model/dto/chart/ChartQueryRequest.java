package com.yupi.springbootinit.model.dto.chart;

import com.yupi.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 查询请求
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private String name;
    private Long id;

    /**
     * 分析目标
     */
    private String goal;



    private String chartType;



    private Long userId;



    private static final long serialVersionUID = 1L;
}