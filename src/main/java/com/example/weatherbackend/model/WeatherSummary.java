package com.example.weatherbackend.model;

public record WeatherSummary(
        double averageSurfacePressure,
        double averageSunshineDuration,
        double minTemperature,
        double maxTemperature,
        String weatherSummary
) {
}