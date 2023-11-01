package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;

/**
 * @Entity com.yupi.springbootinit.model.entity.Chart
 */
public interface ChartMapper extends BaseMapper<Chart> {
    /*
     * 方法的返回类型是 List<Map<String, Object>>,
     * 表示返回的是一个由多个 map 组成的集合,每个map代表了一行查询结果，
     * 并将其封装成了一组键值对形式的对象。其中,String类型代表了键的类型为字符串，
     * Object 类型代表了值的类型为任意对象,使得这个方法可以适应不同类型的数据查询。
     *
     */
    List<Map<String, Object>> queryChartData(String querySql);

}




