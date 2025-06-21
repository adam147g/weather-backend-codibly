package com.example.weatherbackend.client;

import com.example.weatherbackend.exceptions.WeatherDataNotFoundException;
import com.example.weatherbackend.model.WeatherResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenMeteoApiClient implements WeatherApiClient {

    @Value("${open-meteo.api.base-url}")
    private String openMeteoBaseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public WeatherResponse getWeatherData(double latitude, double longitude, String dailyParameters, String hourlyParameters, String timezone) {
        try {
            String string_latitude = String.format("%.6f", latitude).replace(",", ".");
            String string_longitude = String.format("%.6f", longitude).replace(",", ".");

            String url = String.format(
                    "%s?latitude=%s&longitude=%s&daily=%s&hourly=%s&timezone=%s",
                    openMeteoBaseUrl, string_latitude, string_longitude, dailyParameters, hourlyParameters, timezone);

            WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);
            if (response == null) {
                throw new WeatherDataNotFoundException("Nie udało się pobrać danych pogodowych z zewnętrznego API.");
            }
            return response;
        } catch (RestClientException e) {
            throw new WeatherDataNotFoundException("Błąd połączenia z zewnętrznym API Open-Meteo: " + e.getMessage());
        }
    }
}