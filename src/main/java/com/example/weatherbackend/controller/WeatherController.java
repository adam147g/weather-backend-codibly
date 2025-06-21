package com.example.weatherbackend.controller;

import com.example.weatherbackend.model.DailyForecast;
import com.example.weatherbackend.model.WeatherSummary;
import com.example.weatherbackend.service.WeatherService;
import com.example.weatherbackend.validation.CoordinateValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
@RestController
@RequestMapping("/api/weather")
@Validated
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private CoordinateValidator coordinateValidator; // Wstrzykujemy walidator

    @GetMapping("/7-day-forecast")
    public ResponseEntity<Map<String, Object>> get7DayForecast(
            @RequestParam String latitude,
            @RequestParam String longitude) {
        // Walidacja i parsowanie koordynatów za pomocą nowej klasy
        double lat = coordinateValidator.parseAndValidateCoordinate(latitude, "Szerokość geograficzna", -90.0, 90.0);
        double lon = coordinateValidator.parseAndValidateCoordinate(longitude, "Długość geograficzna", -180.0, 180.0);

        List<DailyForecast> forecast = weatherService.get7DayForecast(lat, lon);
        Map<String, Object> response = new HashMap<>();
        response.put("days", forecast);
        response.put("daily_units", weatherService.getDailyUnits());

        return ResponseEntity.ok(response);
        // Obsługa wyjątków została przeniesiona do GlobalExceptionHandler
    }

    @GetMapping("/weekly-summary")
    public ResponseEntity<Map<String, Object>> getWeekSummary(
            @RequestParam String latitude,
            @RequestParam String longitude) {
        // Walidacja i parsowanie koordynatów za pomocą nowej klasy
        double lat = coordinateValidator.parseAndValidateCoordinate(latitude, "Szerokość geograficzna", -90.0, 90.0);
        double lon = coordinateValidator.parseAndValidateCoordinate(longitude, "Długość geograficzna", -180.0, 180.0);

        WeatherSummary summary = weatherService.getWeekSummary(lat, lon);

        Map<String, Object> response = new HashMap<>();
        response.put("weekly_summary", summary);
        response.put("weekly_summary_units", weatherService.getWeeklySummaryUnits());

        return ResponseEntity.ok(response);
        // Obsługa wyjątków została przeniesiona do GlobalExceptionHandler
    }
}