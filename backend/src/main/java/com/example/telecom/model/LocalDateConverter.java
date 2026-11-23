package com.example.telecom.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

/**
 * Stores calendar dates as ISO "yyyy-MM-dd" text. The SQLite driver would
 * otherwise write a LocalDate as a timestamp string with a time part, which
 * does not compare cleanly with plain dates.
 */
@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.substring(0, 10));
    }
}
