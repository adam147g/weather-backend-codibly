package com.example.weatherbackend.client;

import com.example.weatherbackend.model.WeatherResponse;

public interface WeatherApiClient {
    WeatherResponse getWeatherData(double latitude, double longitude, String dailyParameters, String hourlyParameters, String timezone);
}