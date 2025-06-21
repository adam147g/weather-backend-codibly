package com.example.weatherbackend.model;

public record DailyForecast(
        String date,
        int weatherCode,
        double minTemperature,
        double maxTemperature,
        double estimatedEnergy
) {
}