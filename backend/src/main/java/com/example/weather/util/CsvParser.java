package com.example.weather.util;

import com.example.weather.dto.ParsedCsv;
import com.example.weather.exception.CsvValidationException;
import com.example.weather.model.WeatherRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class CsvParser {
    private static final Set<String> REQUIRED_HEADERS = Set.of("日期", "地区", "气温");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final BigDecimal MIN_REASONABLE_TEMPERATURE = new BigDecimal("-80");
    private static final BigDecimal MAX_REASONABLE_TEMPERATURE = new BigDecimal("60");

    public ParsedCsv parse(MultipartFile file) {
        validateFile(file);

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            if (content.isBlank()) {
                throw new CsvValidationException("文件不能为空");
            }
            return parseContent(content);
        } catch (CsvValidationException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw new CsvValidationException("CSV 文件解析失败，请检查文件格式");
        }
    }

    private ParsedCsv parseContent(String content) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (CSVParser parser = format.parse(new StringReader(content))) {
            List<String> missingHeaders = REQUIRED_HEADERS.stream()
                    .filter(header -> !parser.getHeaderMap().containsKey(header))
                    .sorted()
                    .toList();
            if (!missingHeaders.isEmpty()) {
                throw new CsvValidationException("CSV 缺少必需表头：" + String.join("、", missingHeaders));
            }

            List<WeatherRecord> records = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            List<Integer> abnormalLines = new ArrayList<>();

            for (CSVRecord csvRecord : parser) {
                int lineNumber = Math.toIntExact(csvRecord.getRecordNumber() + 1);
                parseRecord(csvRecord, lineNumber, records, errors, abnormalLines);
            }

            if (!errors.isEmpty()) {
                throw new CsvValidationException(errors);
            }
            if (records.isEmpty()) {
                throw new CsvValidationException("CSV 中没有可导入的数据记录");
            }

            List<String> warnings = abnormalLines.isEmpty()
                    ? List.of()
                    : List.of("检测到 " + abnormalLines.size() + " 条可能异常的气温记录（第 "
                    + abnormalLines.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("、"))
                    + " 行）");
            return new ParsedCsv(List.copyOf(records), abnormalLines.size(), warnings);
        }
    }

    private void parseRecord(
            CSVRecord csvRecord,
            int lineNumber,
            List<WeatherRecord> records,
            List<String> errors,
            List<Integer> abnormalLines
    ) {
        String dateText = readField(csvRecord, "日期");
        String region = readField(csvRecord, "地区");
        String temperatureText = readField(csvRecord, "气温");
        boolean valid = true;
        LocalDate date = null;
        BigDecimal temperature = null;

        if (dateText.isEmpty()) {
            errors.add("第 " + lineNumber + " 行：日期不能为空");
            valid = false;
        } else {
            try {
                date = LocalDate.parse(dateText, DATE_FORMATTER);
            } catch (DateTimeParseException exception) {
                errors.add("第 " + lineNumber + " 行：日期格式错误，应为 yyyy-MM-dd");
                valid = false;
            }
        }

        if (region.isEmpty()) {
            errors.add("第 " + lineNumber + " 行：地区不能为空");
            valid = false;
        }

        if (temperatureText.isEmpty()) {
            errors.add("第 " + lineNumber + " 行：气温不能为空");
            valid = false;
        } else {
            try {
                temperature = new BigDecimal(temperatureText);
            } catch (NumberFormatException exception) {
                errors.add("第 " + lineNumber + " 行：气温不是有效数字");
                valid = false;
            }
        }

        if (valid) {
            records.add(new WeatherRecord(date, region, temperature));
            if (temperature.compareTo(MIN_REASONABLE_TEMPERATURE) < 0
                    || temperature.compareTo(MAX_REASONABLE_TEMPERATURE) > 0) {
                abnormalLines.add(lineNumber);
            }
        }
    }

    private String readField(CSVRecord csvRecord, String header) {
        return csvRecord.isSet(header) ? csvRecord.get(header).trim() : "";
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CsvValidationException("文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new CsvValidationException("文件后缀必须为 .csv");
        }
    }
}
