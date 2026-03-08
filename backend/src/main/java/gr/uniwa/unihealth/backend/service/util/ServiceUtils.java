package gr.uniwa.unihealth.backend.service.util;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.*;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * Contains util functions for services.
 *
 * @author omaro
 */
@UtilityClass
public class ServiceUtils {

  /**
   * Finds an expression within a path.
   *
   * @param <T>
   * @param rootPath     The root entity path.
   * @param propertyPath The property chain (dot separated) to return.
   * @return The expression found.
   */
  @SuppressWarnings("unchecked")
  public <T extends Expression<?>> T findExpressionInPath(EntityPath<?> rootPath,
      String propertyPath) {
    Expression<?> result = rootPath;

    String[] propertyParts = propertyPath.split("\\.");
    for (String propertyPart : propertyParts) {
      try {
        Field field = result.getClass().getField(propertyPart);
        result = (Expression<?>) field.get(result);

        if (result instanceof CollectionPathBase) {
          result = ((CollectionPathBase<?, ?, ?>) result).any();
        }
      } catch (NoSuchFieldException | SecurityException | IllegalArgumentException |
          IllegalAccessException e) {
        throw new IllegalArgumentException(e);
      }
    }

    return (T) result;
  }

  /**
   * Builds a predicate for searching in a decimal field. It allows searching for the numeric value
   * in different formats (with or without spaces, with "." or ",").
   *
   * @param search The search string.
   * @param path   The numeric path to search in.
   * @return A BooleanExpression representing the search predicate, or null if the search string is
   * empty or invalid.
   */
  public BooleanExpression buildDecimalPredicate(String search, NumberPath<BigDecimal> path) {
    String qNoSpaces = search.replaceAll("\\s+", "");
    // Avoid CAST operations for text searches.
    if (!qNoSpaces.matches(".*\\d.*")) {
      return null;
    }
    // support both "." and ","
    String qDot = qNoSpaces.replace(',', '.');
    String qComma = qNoSpaces.replace('.', ',');

    // Convert numeric column to string to be searchable (since it is stored as bigDecimal)
    // This will have impact on big data as it is applied in whole table. Is there a better way rather than applying functions?
    StringExpression pathText = Expressions.stringTemplate("CAST({0} AS text)", path);
    StringExpression amountTextComma =
        Expressions.stringTemplate("REPLACE(CAST({0} AS text), '.', ',')", path);

    return pathText.contains(qDot).or(amountTextComma.contains(qComma))
        .or(pathText.contains(qNoSpaces)).or(amountTextComma.contains(qNoSpaces));
  }

  /**
   * Normalises a string by trimming it and replacing multiple consecutive whitespace characters
   * with a single space.
   *
   * @param value The string to normalise.
   * @return The normalised string, or null if the input is blank.
   */
  public String normalise(String value) {
    return StringUtils.isBlank(value) ? null : value.trim().replaceAll("\\s{2,}", " ");
  }
}
