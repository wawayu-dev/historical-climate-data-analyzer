package com.example.weather.model;

import java.math.BigDecimal;

public record TemperatureAnomaly(
        int year,
        int month,
        String region,
        BigDecimal monthlyAverage,
        BigDecimal baselineAverage,
        BigDecimal anomaly
) {
}
