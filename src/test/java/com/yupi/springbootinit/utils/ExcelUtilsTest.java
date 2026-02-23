package com.yupi.springbootinit.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelUtilsTest {

    @Test
    void fileToCsvShouldReturnEmptyForUnsupportedSuffix() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.txt",
                "text/plain",
                "name,score\nalice,95".getBytes(StandardCharsets.UTF_8)
        );

        String result = ExcelUtils.fileToCsv(file);

        assertEquals("", result);
    }

    @Test
    void fileToCsvShouldNormalizeCsvAndRemoveBom() {
        String csvWithBom = "\uFEFFname,score\nalice,95\n\nbob,88\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.csv",
                "text/csv",
                csvWithBom.getBytes(StandardCharsets.UTF_8)
        );

        String result = ExcelUtils.fileToCsv(file);

        assertEquals("name,score\nalice,95\nbob,88\n", result);
    }

    @Test
    void fileToCsvShouldReturnEmptyWhenExcelContentIsInvalid() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "broken.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not-an-excel-binary".getBytes(StandardCharsets.UTF_8)
        );

        assertDoesNotThrow(() -> {
            String result = ExcelUtils.fileToCsv(file);
            assertEquals("", result);
        });
    }
}
