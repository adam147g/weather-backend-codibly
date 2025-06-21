package com.example.weatherbackend;

import com.example.weatherbackend.client.WeatherApiClient;
import com.example.weatherbackend.exceptions.WeatherDataNotFoundException;
import com.example.weatherbackend.model.DailyForecast;
import com.example.weatherbackend.model.WeatherResponse;
import com.example.weatherbackend.model.WeatherSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WeatherControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // Do serializacji/deserializacji JSON

    @MockBean
    private WeatherApiClient weatherApiClient; // Mockujemy klienta zewnętrznego API

    private WeatherResponse mockWeatherResponse;

    // Poprawna wartość dailyParameters z application.properties
    private static final String DAILY_PARAMETERS_CONFIG = "weather_code,temperature_2m_min,temperature_2m_max,sunshine_duration,rain_sum";
    // Poprawna wartość hourlyParameters z application.properties
    private static final String HOURLY_PARAMETERS_CONFIG = "surface_pressure";
    // Poprawna wartość timezone z application.properties
    private static final String TIMEZONE_CONFIG = "auto";
    @BeforeEach
    void setUp() {
        // Przygotowujemy mockowaną odpowiedź z API Open-Meteo
        mockWeatherResponse = new WeatherResponse(
                new WeatherResponse.Daily(
                        Arrays.asList("2024-11-20", "2024-11-21", "2024-11-22"),
                        Arrays.asList(1, 2, 1), // weather_code
                        Arrays.asList(5.0, 6.0, 7.0), // temp_min
                        Arrays.asList(15.0, 16.0, 17.0), // temp_max
                        Arrays.asList(36000.0, 30000.0, 42000.0), // sunshine_duration (in seconds)
                        Arrays.asList(0.0, 1.0, 0.0) // rain_sum
                ),
                new WeatherResponse.Hourly(
                        Arrays.asList(1013.0, 1015.0, 1017.0) // surface_pressure
                )
        );

        // Ustawiamy oczekiwane zachowanie mocka WeatherApiClient dla get7DayForecast
        when(weatherApiClient.getWeatherData(
                anyDouble(), anyDouble(),
                eq(DAILY_PARAMETERS_CONFIG), // Używamy stałej z poprawną kolejnością
                eq(""), // Empty for 7-day forecast
                eq(TIMEZONE_CONFIG))) // Używamy stałej
                .thenReturn(mockWeatherResponse);

        // Ustawiamy oczekiwane zachowanie mocka WeatherApiClient dla getWeekSummary
        when(weatherApiClient.getWeatherData(
                anyDouble(), anyDouble(),
                eq(DAILY_PARAMETERS_CONFIG), // Używamy stałej z poprawną kolejnością
                eq(HOURLY_PARAMETERS_CONFIG), // Używamy stałej
                eq(TIMEZONE_CONFIG))) // Używamy stałej
                .thenReturn(mockWeatherResponse);
    }

    @Test
    void get7DayForecast_validCoordinates_returnsOkAndForecast() throws Exception {
        mockMvc.perform(get("/api/weather/7-day-forecast")
                        .param("latitude", "52.2298")
                        .param("longitude", "21.0118")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(3)))
                .andExpect(jsonPath("$.days[0].date", is("2024-11-20")))
                .andExpect(jsonPath("$.days[0].weatherCode", is(1)))
                .andExpect(jsonPath("$.days[0].minTemperature", closeTo(5.0, 0.01)))
                .andExpect(jsonPath("$.days[0].maxTemperature", closeTo(15.0, 0.01)))
                // Estimated energy for 36000 seconds (10 hours) sunshine: 2.5 * 10 * 0.2 = 5.0
                .andExpect(jsonPath("$.days[0].estimatedEnergy", closeTo(5.0, 0.01)))
                .andExpect(jsonPath("$.days[1].date", is("2024-11-21")))
                .andExpect(jsonPath("$.days[1].estimatedEnergy", closeTo(4.167, 0.01))) // 30000s = 8.33h => 2.5 * 8.33 * 0.2 = 4.167
                .andExpect(jsonPath("$.days[2].estimatedEnergy", closeTo(5.834, 0.01))) // 42000s = 11.66h => 2.5 * 11.66 * 0.2 = 5.834
                .andExpect(jsonPath("$.daily_units.date", is("YYYY-MM-DD")))
                .andExpect(jsonPath("$.daily_units.weatherCode", is("wmo code")));
    }

    @Test
    void get7DayForecast_invalidLatitude_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/weather/7-day-forecast")
                        .param("latitude", "abc") // Invalid input
                        .param("longitude", "21.0118")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Szerokość geograficzna musi być liczbą zmiennoprzecinkową.")));
    }

    @Test
    void get7DayForecast_missingLongitude_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/weather/7-day-forecast")
                        .param("latitude", "52.2298")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Brak wymaganego parametru: 'longitude'.")));
    }

    @Test
    void get7DayForecast_apiReturnsNoData_returnsNotFound() throws Exception {
        // Zmieniamy zachowanie mocka dla tego konkretnego testu
        // Używamy eq(0.0) dla konkretnych koordynatów, aby zapewnić dokładne dopasowanie
        when(weatherApiClient.getWeatherData(
                eq(0.0), eq(0.0), // Zmiana: jawne określenie 0.0 dla latitude i longitude
                eq(DAILY_PARAMETERS_CONFIG),
                eq(""),
                eq(TIMEZONE_CONFIG)))
                .thenThrow(new WeatherDataNotFoundException("Nie udało się pobrać danych pogodowych z zewnętrznego API (odpowiedź jest pusta)."));

        mockMvc.perform(get("/api/weather/7-day-forecast")
                        .param("latitude", "0.0") // Używamy tych samych koordynatów, co w mocku
                        .param("longitude", "0.0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Nie udało się pobrać danych pogodowych z zewnętrznego API (odpowiedź jest pusta).")));
    }


    @Test
    void getWeekSummary_validCoordinates_returnsOkAndSummary() throws Exception {
        mockMvc.perform(get("/api/weather/weekly-summary")
                        .param("latitude", "52.2298")
                        .param("longitude", "21.0118")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekly_summary.averageSurfacePressure", closeTo(1015.0, 0.01)))
                .andExpect(jsonPath("$.weekly_summary.averageSunshineDuration", closeTo(36000.0, 0.01)))
                .andExpect(jsonPath("$.weekly_summary.minTemperature", closeTo(5.0, 0.01)))
                .andExpect(jsonPath("$.weekly_summary.maxTemperature", closeTo(17.0, 0.01)))
                .andExpect(jsonPath("$.weekly_summary.weatherSummary", is("Not rainy")))
                .andExpect(jsonPath("$.weekly_summary_units.averageSurfacePressure", is("hPa")));
    }

    @Test
    void getWeekSummary_invalidLongitudeRange_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/weather/weekly-summary")
                        .param("latitude", "10.0")
                        .param("longitude", "190.0") // Out of range
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Długość geograficzna musi być w przedziale [-180.0, 180.0].")));
    }
}