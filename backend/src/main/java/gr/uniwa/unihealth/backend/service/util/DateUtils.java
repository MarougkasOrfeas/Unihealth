package gr.uniwa.unihealth.backend.service.util;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@UtilityClass
public class DateUtils {

  public final ZoneOffset APP_ZONE = ZoneOffset.UTC;

  public final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  public final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  public final DateTimeFormatter EXPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  public final DateTimeFormatter EXPORT_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

  public final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private final String DATE_TIME_EXPRESSION = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}";

  public final Pattern DATE_TIME_RANGE_FILTER_PATTERN =
      Pattern.compile("^(" + DATE_TIME_EXPRESSION + ")-(" + DATE_TIME_EXPRESSION + ")$");

  public long atStartOfDayToEpochMillis(LocalDate date) {
    return date.atStartOfDay(APP_ZONE).toInstant().toEpochMilli();
  }

  public LocalDate toLocalDate(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis).atZone(APP_ZONE).toLocalDate();
  }

  public LocalDateTime toLocalDateTime(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis).atZone(APP_ZONE).toLocalDateTime();
  }

  public LocalDate parseSearchDate(String value) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException ignored) {
      try {
        return LocalDate.parse(value, DATE_FORMATTER);
      } catch (DateTimeParseException ignoredAgain) {
        return null;
      }
    }
  }
}
