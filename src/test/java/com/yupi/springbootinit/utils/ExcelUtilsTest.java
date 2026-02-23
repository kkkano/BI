package com.yupi.springbootinit.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelUtilsTest {

    @Test
    void fileToCsv_shouldConvertCsvFileAndRemoveBom() {
        String csvContent = "\uFEFFdate,users\n2026-01-01,100\n\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        String result = ExcelUtils.fileToCsv(file);

        assertEquals("date,users\n2026-01-01,100\n", result);
    }

    @Test
    void fileToCsv_shouldConvertXlsxFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createExcelBytes(ExcelTypeEnum.XLSX));

        String result = ExcelUtils.fileToCsv(file);

        assertEquals("date,users\n2026-01-01,100\n", result);
    }

    @Test
    void fileToCsv_shouldReturnEmptyWhenSuffixNotSupported() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));

        String result = ExcelUtils.fileToCsv(file);

        assertTrue(result.isEmpty());
    }

    private byte[] createExcelBytes(ExcelTypeEnum excelTypeEnum) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EasyExcel.write(outputStream)
                .excelType(excelTypeEnum)
                .head(buildHead())
                .sheet("sheet1")
                .doWrite(buildRows());
        return outputStream.toByteArray();
    }

    private List<List<String>> buildHead() {
        return Arrays.asList(
                Collections.singletonList("date"),
                Collections.singletonList("users")
        );
    }

    private List<List<String>> buildRows() {
        return Collections.singletonList(Arrays.asList("2026-01-01", "100"));
    }
}
