package gr.uniwa.unihealth.backend.model.converter;

import io.micrometer.common.util.StringUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Converter
public class LabelListConverter implements AttributeConverter<List<String>, String> {

  private static final String SEPARATOR = ",";

  @Override
  public String convertToDatabaseColumn(List<String> labels) {
    if (labels == null) {
      throw new IllegalArgumentException("Parameter 'labels' cannot be NULL.");
    }
    List<String> processed = labels.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(entry -> !entry.isEmpty())
        .toList();
    if (processed.isEmpty()) {
      throw new IllegalArgumentException(
          "Parameter 'labels' must contain at least one non-blank entry.");
    }
    return String.join(SEPARATOR + " ", processed);
  }

  @Override
  public List<String> convertToEntityAttribute(String labels) {
    if (StringUtils.isBlank(labels)) {
      throw new IllegalArgumentException("Parameter 'labels' cannot be blank.");
    }
    return Arrays.stream(labels.split(SEPARATOR))
        .map(String::trim)
        .filter(entry -> !entry.isEmpty())
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
