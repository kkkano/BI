package com.yupi.springbootinit.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.SneakyThrows;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

/**
 * Excel 相关工具类
 */
public class ExcelUtils {
    public static String excelToCsv(MultipartFile multipartFile) {
        File file = null;
        try {
            file = ResourceUtils.getFile("classpath:网站数据.xlsx");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<Map<Integer, String>> list = EasyExcel.read(file)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet()
                .headRowNumber(0)
                .doReadSync();
        System.out.println(list);
        return "";
    }

    public static void main(String[] args) {
        excelToCsv(null);
    }
}