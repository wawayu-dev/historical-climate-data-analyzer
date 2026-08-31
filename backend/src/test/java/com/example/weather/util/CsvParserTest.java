package com.example.weather.util;

import com.example.weather.dto.ParsedCsv;
import com.example.weather.exception.CsvValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParserTest {
    private final CsvParser parser = new CsvParser();

    @Test
    void parsesUtf8BomAndReportsSuspiciousTemperatures() {
        String csv = "\uFEFF日期,地区,气温\n2020-01-01,北京,2.5\n2020-01-02,北京,61\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "weather.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ParsedCsv parsed = parser.parse(file);

        assertThat(parsed.records()).hasSize(2);
        assertThat(parsed.abnormalCount()).isEqualTo(1);
        assertThat(parsed.warnings().get(0)).contains("第 3 行");
    }

    @Test
    void collectsMultipleRowErrors() {
        String csv = "日期,地区,气温\n2020-13-01,北京,2.5\n2020-01-02,,abc\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "weather.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(CsvValidationException.class)
                .hasMessageContaining("第 2 行：日期格式错误")
                .hasMessageContaining("第 3 行：地区不能为空")
                .hasMessageContaining("第 3 行：气温不是有效数字");
    }
}
