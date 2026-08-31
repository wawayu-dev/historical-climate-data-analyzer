package com.example.weather.dto;

import java.util.List;

public record UploadResultDTO(
        int rawRecordCount,
        int resultCount,
        List<String> regions,
        Integer minYear,
        Integer maxYear,
        int abnormalCount,
        List<String> warnings
) {
}
