package com.example.weather.store;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class WeatherDataStore {
    private final AtomicReference<WeatherSnapshot> snapshot = new AtomicReference<>(WeatherSnapshot.empty());

    public WeatherSnapshot current() {
        return snapshot.get();
    }

    public void replace(WeatherSnapshot newSnapshot) {
        snapshot.set(newSnapshot);
    }
}
