package com.example.weatherbackend.validation;

import com.example.weatherbackend.exceptions.InvalidInputException;
import org.springframework.stereotype.Component;

@Component
public class CoordinateValidator {
    public double parseAndValidateCoordinate(String coordinate, String coordinateName, double minValue, double maxValue) throws InvalidInputException {
        try {
            double value = Double.parseDouble(coordinate);

            // Walidacja zakresu
            if (value < minValue || value > maxValue) {
                throw new InvalidInputException(coordinateName + " musi być w przedziale [" + minValue + ", " + maxValue + "].");
            }

            return value;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(coordinateName + " musi być liczbą zmiennoprzecinkową.");
        }
    }
}
