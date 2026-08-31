package com.example.weather.service;

import com.example.weather.dto.MetadataDTO;
import com.example.weather.dto.ParsedCsv;
import com.example.weather.dto.UploadResultDTO;
import com.example.weather.exception.BadRequestException;
import com.example.weather.model.TemperatureAnomaly;
import com.example.weather.model.WeatherRecord;
import com.example.weather.store.WeatherDataStore;
import com.example.weather.store.WeatherSnapshot;
import com.example.weather.util.CsvParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WeatherAnalysisService {
    private static final int CALCULATION_SCALE = 10;
    private static final int DISPLAY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final CsvParser csvParser;
    private final WeatherDataStore dataStore;

    public WeatherAnalysisService(CsvParser csvParser, WeatherDataStore dataStore) {
        this.csvParser = csvParser;
        this.dataStore = dataStore;
    }

    public UploadResultDTO upload(MultipartFile file) {
        ParsedCsv parsedCsv = csvParser.parse(file);
        List<TemperatureAnomaly> results = analyze(parsedCsv.records());
        MetadataDTO metadata = buildMetadata(parsedCsv, results);
        WeatherSnapshot snapshot = new WeatherSnapshot(
                List.copyOf(parsedCsv.records()),
                List.copyOf(results),
                metadata
        );
        dataStore.replace(snapshot);
        return new UploadResultDTO(
                metadata.rawRecordCount(), metadata.resultCount(), metadata.regions(),
                metadata.minYear(), metadata.maxYear(), metadata.abnormalCount(), metadata.warnings()
        );
    }

    public List<TemperatureAnomaly> analyze(List<WeatherRecord> records) {
        Map<MonthlyKey, AverageAccumulator> monthlyGroups = new LinkedHashMap<>();
        for (WeatherRecord record : records) {
            MonthlyKey key = new MonthlyKey(record.region(), record.date().getYear(), record.date().getMonthValue());
            monthlyGroups.computeIfAbsent(key, ignored -> new AverageAccumulator()).add(record.temperature());
        }

        Map<MonthlyKey, BigDecimal> monthlyAverages = new LinkedHashMap<>();
        Map<RegionMonthKey, AverageAccumulator> baselineGroups = new LinkedHashMap<>();
        monthlyGroups.forEach((key, accumulator) -> {
            BigDecimal monthlyAverage = accumulator.average();
            monthlyAverages.put(key, monthlyAverage);
            baselineGroups.computeIfAbsent(new RegionMonthKey(key.region(), key.month()), ignored -> new AverageAccumulator())
                    .add(monthlyAverage);
        });

        Map<RegionMonthKey, BigDecimal> baselines = new LinkedHashMap<>();
        baselineGroups.forEach((key, accumulator) -> baselines.put(key, accumulator.average()));

        return monthlyAverages.entrySet().stream()
                .map(entry -> {
                    MonthlyKey key = entry.getKey();
                    BigDecimal monthlyAverage = entry.getValue();
                    BigDecimal baseline = baselines.get(new RegionMonthKey(key.region(), key.month()));
                    return new TemperatureAnomaly(
                            key.year(),
                            key.month(),
                            key.region(),
                            display(monthlyAverage),
                            display(baseline),
                            display(monthlyAverage.subtract(baseline))
                    );
                })
                .sorted(Comparator.comparingInt(TemperatureAnomaly::year)
                        .thenComparingInt(TemperatureAnomaly::month)
                        .thenComparing(TemperatureAnomaly::region))
                .toList();
    }

    public MetadataDTO metadata() {
        return dataStore.current().metadata();
    }

    public List<TemperatureAnomaly> results(List<String> regions, Integer startYear, Integer endYear, Integer month) {
        validateFilters(startYear, endYear, month);
        Set<String> regionSet = normalizeRegions(regions);
        return dataStore.current().analysisResults().stream()
                .filter(result -> regionSet.isEmpty() || regionSet.contains(result.region()))
                .filter(result -> startYear == null || result.year() >= startYear)
                .filter(result -> endYear == null || result.year() <= endYear)
                .filter(result -> month == null || result.month() == month)
                .toList();
    }

    public byte[] exportCsv(List<String> regions, Integer startYear, Integer endYear, Integer month) {
        List<TemperatureAnomaly> filteredResults = results(regions, startYear, endYear, month);
        StringBuilder output = new StringBuilder("\uFEFF");
        try (CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT)) {
            printer.printRecord("年份", "月份", "地区", "月平均气温", "历史同期平均气温", "距平值");
            for (TemperatureAnomaly result : filteredResults) {
                printer.printRecord(
                        result.year(), result.month(), result.region(),
                        result.monthlyAverage().toPlainString(),
                        result.baselineAverage().toPlainString(),
                        result.anomaly().toPlainString()
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("CSV 导出失败", exception);
        }
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    private MetadataDTO buildMetadata(ParsedCsv parsedCsv, List<TemperatureAnomaly> results) {
        List<String> regions = parsedCsv.records().stream().map(WeatherRecord::region).distinct().sorted().toList();
        List<Integer> years = parsedCsv.records().stream().map(record -> record.date().getYear()).distinct().sorted().toList();
        List<Integer> months = parsedCsv.records().stream().map(record -> record.date().getMonthValue()).distinct().sorted().toList();
        return new MetadataDTO(
                parsedCsv.records().size(), results.size(), regions,
                years.isEmpty() ? null : years.get(0),
                years.isEmpty() ? null : years.get(years.size() - 1),
                months, parsedCsv.abnormalCount(), parsedCsv.warnings()
        );
    }

    private void validateFilters(Integer startYear, Integer endYear, Integer month) {
        if (month != null && (month < 1 || month > 12)) {
            throw new BadRequestException("月份必须在 1 到 12 之间");
        }
        if (startYear != null && endYear != null && startYear > endYear) {
            throw new BadRequestException("起始年份不能大于结束年份");
        }
    }

    private Set<String> normalizeRegions(List<String> regions) {
        if (regions == null) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        regions.stream()
                .flatMap(region -> java.util.Arrays.stream(region.split(",")))
                .map(String::trim)
                .filter(region -> !region.isEmpty())
                .forEach(normalized::add);
        return normalized;
    }

    private BigDecimal display(BigDecimal value) {
        return value.setScale(DISPLAY_SCALE, ROUNDING_MODE);
    }

    private record MonthlyKey(String region, int year, int month) {
    }

    private record RegionMonthKey(String region, int month) {
    }

    private static final class AverageAccumulator {
        private BigDecimal sum = BigDecimal.ZERO;
        private int count;

        void add(BigDecimal value) {
            sum = sum.add(value);
            count++;
        }

        BigDecimal average() {
            return sum.divide(BigDecimal.valueOf(count), CALCULATION_SCALE, ROUNDING_MODE);
        }
    }
}
