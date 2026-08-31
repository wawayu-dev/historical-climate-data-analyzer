package com.example.weather.dto;

import com.example.weather.model.WeatherRecord;

import java.util.List;

public record ParsedCsv(List<WeatherRecord> records, int abnormalCount, List<String> warnings) {
}
