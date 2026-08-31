package com.example.weather.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeatherRecord(LocalDate date, String region, BigDecimal temperature) {
}
