package com.financeai.finance_management.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

@Component
@Converter
public class DateTimeEpochConverter implements AttributeConverter<String, Long> {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

  @Override
  public Long convertToDatabaseColumn(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    try {
      LocalDateTime dateTime = LocalDateTime.parse(dateStr, FORMATTER);
      return dateTime.atZone(SYSTEM_ZONE).toInstant().toEpochMilli();
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  @Override
  public String convertToEntityAttribute(Long epoch) {
    if (epoch == null) {
      return null;
    }
    return Instant.ofEpochMilli(epoch).atZone(SYSTEM_ZONE).toLocalDateTime().format(FORMATTER);
  }
}
