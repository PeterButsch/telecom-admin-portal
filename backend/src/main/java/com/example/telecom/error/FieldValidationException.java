package com.example.telecom.error;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A business-rule validation failure on one or more request fields, reported
 * like bean validation errors.
 */
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public FieldValidationException(Map<String, String> errors) {
        super("One or more fields are invalid.");
        this.errors = new LinkedHashMap<>(errors);
    }

    public FieldValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    public Map<String, String> getErrors() {
        return Map.copyOf(errors);
    }
}
