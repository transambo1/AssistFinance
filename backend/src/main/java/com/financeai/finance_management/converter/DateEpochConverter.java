package com.financeai.finance_management.converter;

import jakarta.persistence.AttributeConverter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Component
public class DateEpochConverter implements AttributeConverter<String, Long> {

  @Override
  public Long convertToDatabaseColumn(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  @Override
  public String convertToEntityAttribute(Long epoch) {
    if (epoch == null) {
      return null;
    }
    return java.time.Instant.ofEpochMilli(epoch).atZone(ZoneOffset.UTC).toLocalDate().toString();
  }
}
