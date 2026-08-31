package com.example.weather.exception;

import java.util.List;

public class CsvValidationException extends RuntimeException {
    private final List<String> errors;

    public CsvValidationException(String error) {
        this(List.of(error));
    }

    public CsvValidationException(List<String> errors) {
        super(String.join("\n", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
