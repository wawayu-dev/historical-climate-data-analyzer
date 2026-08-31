package com.example.weather.service;

import com.example.weather.model.TemperatureAnomaly;
import com.example.weather.model.WeatherRecord;
import com.example.weather.store.WeatherDataStore;
import com.example.weather.util.CsvParser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeatherAnalysisServiceTest {
    private final WeatherAnalysisService service = new WeatherAnalysisService(new CsvParser(), new WeatherDataStore());

    @Test
    void calculatesMonthlyBaselineAndAnomalyUsingMonthlyAverages() {
        List<WeatherRecord> records = List.of(
                record("2020-01-15", "北京", "2.5"),
                record("2020-01-20", "北京", "3.1"),
                record("2021-01-15", "北京", "1.8"),
                record("2021-01-20", "北京", "2.4")
        );

        List<TemperatureAnomaly> results = service.analyze(records);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).monthlyAverage()).isEqualByComparingTo("2.80");
        assertThat(results.get(0).baselineAverage()).isEqualByComparingTo("2.45");
        assertThat(results.get(0).anomaly()).isEqualByComparingTo("0.35");
        assertThat(results.get(1).monthlyAverage()).isEqualByComparingTo("2.10");
        assertThat(results.get(1).baselineAverage()).isEqualByComparingTo("2.45");
        assertThat(results.get(1).anomaly()).isEqualByComparingTo("-0.35");
    }

    @Test
    void keepsRegionsAndMonthsIndependent() {
        List<WeatherRecord> records = List.of(
                record("2020-01-01", "北京", "0"),
                record("2021-01-01", "北京", "10"),
                record("2020-02-01", "北京", "30"),
                record("2020-01-01", "上海", "100")
        );

        List<TemperatureAnomaly> results = service.analyze(records);

        assertThat(results).extracting(TemperatureAnomaly::anomaly)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("-5.00"), new BigDecimal("0.00"), new BigDecimal("5.00"));
    }

    @Test
    void invalidUploadDoesNotReplaceExistingSnapshot() {
        MockMultipartFile validFile = csvFile("日期,地区,气温\n2020-01-01,北京,1\n");
        MockMultipartFile invalidFile = csvFile("日期,地区,气温\n错误日期,上海,2\n");

        service.upload(validFile);
        assertThatThrownBy(() -> service.upload(invalidFile)).isInstanceOf(RuntimeException.class);

        assertThat(service.metadata().rawRecordCount()).isEqualTo(1);
        assertThat(service.metadata().regions()).containsExactly("北京");
    }

    private WeatherRecord record(String date, String region, String temperature) {
        return new WeatherRecord(LocalDate.parse(date), region, new BigDecimal(temperature));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "weather.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
