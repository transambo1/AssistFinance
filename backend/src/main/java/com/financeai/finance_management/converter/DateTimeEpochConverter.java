package com.financeai.finance_management.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@Converter
public class DateTimeEpochConverter implements AttributeConverter<String, Long> {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  @Override
  public Long convertToDatabaseColumn(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    try {
      LocalDateTime dateTime = LocalDateTime.parse(dateStr, FORMATTER);
      return dateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  @Override
  public String convertToEntityAttribute(Long epoch) {
    if (epoch == null) {
      return null;
    }
    return Instant.ofEpochMilli(epoch).atZone(ZoneOffset.UTC).toLocalDateTime().format(FORMATTER);
  }
}
