package com.example.weather.store;

import com.example.weather.dto.MetadataDTO;
import com.example.weather.model.TemperatureAnomaly;
import com.example.weather.model.WeatherRecord;

import java.util.List;

public record WeatherSnapshot(
        List<WeatherRecord> rawRecords,
        List<TemperatureAnomaly> analysisResults,
        MetadataDTO metadata
) {
    public static WeatherSnapshot empty() {
        return new WeatherSnapshot(List.of(), List.of(), MetadataDTO.empty());
    }
}
