package com.yupi.springbootinit.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel / CSV 相关工具类
 */
@Slf4j
public class ExcelUtils {

    /**
     * 兼容旧调用入口，统一走 fileToCsv
     */
    public static String excelToCsv(MultipartFile multipartFile) {
        return fileToCsv(multipartFile);
    }

    /**
     * 将上传文件转换为 CSV 文本，支持 xlsx / xls / csv
     */
    public static String fileToCsv(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return "";
        }
        String originalFilename = multipartFile.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            return "";
        }
        String suffix = StringUtils.lowerCase(FileUtil.getSuffix(originalFilename));
        if ("csv".equals(suffix)) {
            return csvToCsv(multipartFile, originalFilename);
        }
        ExcelTypeEnum excelTypeEnum = getExcelTypeEnum(suffix);
        if (excelTypeEnum == null) {
            log.warn("不支持的文件后缀，无法转换为 csv: {}", suffix);
            return "";
        }
        return excelToCsv(multipartFile, excelTypeEnum, originalFilename);
    }

    private static ExcelTypeEnum getExcelTypeEnum(String suffix) {
        if ("xlsx".equals(suffix)) {
            return ExcelTypeEnum.XLSX;
        }
        if ("xls".equals(suffix)) {
            return ExcelTypeEnum.XLS;
        }
        return null;
    }

    private static String csvToCsv(MultipartFile multipartFile, String filename) {
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = bufferedReader.lines()
                    .map(ExcelUtils::removeBom)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (CollUtil.isEmpty(lines)) {
                return "";
            }
            return StringUtils.join(lines, "\n") + "\n";
        } catch (Exception e) {
            log.error("CSV 处理错误 filename={}", filename, e);
            return "";
        }
    }

    private static String removeBom(String line) {
        if (line == null) {
            return null;
        }
        return StringUtils.removeStart(line, "\uFEFF");
    }

    private static String excelToCsv(MultipartFile multipartFile, ExcelTypeEnum excelTypeEnum, String filename) {
        List<Map<Integer, String>> list;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(excelTypeEnum)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (Exception e) {
            log.error("表格处理错误 filename={}, type={}", filename, excelTypeEnum, e);
            return "";
        }
        if (CollUtil.isEmpty(list)) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap<Integer, String>) list.get(0);
        List<String> headerList = headerMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headerList, ",")).append("\n");
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap<Integer, String>) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList, ",")).append("\n");
        }
        return stringBuilder.toString();
    }
}
