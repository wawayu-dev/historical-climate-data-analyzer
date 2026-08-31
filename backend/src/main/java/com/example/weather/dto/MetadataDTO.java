package com.example.weather.dto;

import java.util.List;

public record MetadataDTO(
        int rawRecordCount,
        int resultCount,
        List<String> regions,
        Integer minYear,
        Integer maxYear,
        List<Integer> months,
        int abnormalCount,
        List<String> warnings
) {
    public static MetadataDTO empty() {
        return new MetadataDTO(0, 0, List.of(), null, null, List.of(), 0, List.of());
    }
}
