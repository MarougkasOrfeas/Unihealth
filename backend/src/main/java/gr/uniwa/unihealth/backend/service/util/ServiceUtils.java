package gr.uniwa.unihealth.backend.service.util;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.*;
import gr.uniwa.unihealth.backend.controller.response.FieldDetail;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.model.annotations.UIRef;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains util functions for services.
 *
 * @author omaro
 */
@UtilityClass
public class ServiceUtils {

  public static final List<String> AVAILABLE_LOCALE = Arrays.asList("de", "en");

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
    for (int i = 0; i < propertyParts.length; i++) {
      String propertyPart = propertyParts[i];
      result = findPropertyExpressionOfPath(result, propertyPart);

      if (result instanceof CollectionPathBase && i < propertyParts.length - 1) {
        result = ((CollectionPathBase<?, ?, ?>) result).any();
      }
    }

    return (T) result;
  }

  /**
   * Finds a property value within a dto/bean, following a dot-separated property path (e.g.
   * "office.name"). Mirrors {@link #findExpressionInPath} but for plain bean navigation.
   *
   * @param <T>          The type to be returned.
   * @param root         The root bean instance.
   * @param propertyPath The property chain (dot separated) to return.
   * @return The value found, or null if root is null.
   */
  @SuppressWarnings("unchecked")
  public <T> T findPropertyValueInPath(Object root, String propertyPath) {
    if (root == null) {
      return null;
    }
    BeanWrapper wrapper = new BeanWrapperImpl(root);
    return (T) wrapper.getPropertyValue(propertyPath);
  }

  public Expression<?> findPropertyExpressionOfPath(Expression<?> expression, String property) {
    try {
      Field field = expression.getClass().getField(property);
      return (Expression<?>) field.get(expression);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    }
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

  /**
   * Validates that the input strings have balanced parentheses. If not, throws a
   * {@link QCouldNotSaveException}.
   *
   * @param inputs The strings to validate.
   */
  public void validateParenthesis(String... inputs) {
    if (inputs != null) {
      for (String input : inputs) {
        if (!hasBalancedParentheses(input)) {
          throw ExceptionUtils.createException(QCouldNotSaveException.class,
              "not_balanced_parenthesis", "Not balanced parenthesis: {}", input);
        }
      }
    }
  }

  private boolean hasBalancedParentheses(String input) {
    if (StringUtils.isBlank(input)) {
      return true;
    }

    int depth = 0;
    for (char c : input.toCharArray()) {
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        if (depth == 0) {
          return false; // closing parenthesis when depth == 0
        }
        depth--;
      }
    }
    return depth == 0;
  }

  /**
   * Fetches a generic type from an object instance.
   *
   * @param <T>      The type tp be returned.
   * @param instance The instance that has a defined generic.
   * @param as       The class that is a superclass of instance and declares the generic.
   * @param index    The generic index.
   * @return The return class.
   */
  @SuppressWarnings("unchecked")
  public <T> Class<T> genericType(Object instance, Class<?> as, int index) {
    return Optional.ofNullable(
            (Class<T>) ResolvableType.forInstance(instance).as(as).getGeneric(index).resolve())
        .orElseThrow();
  }

  public String stripEnclosingQuotes(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() >= 2 && ((trimmed.startsWith("\"") && trimmed.endsWith(
        "\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
      return trimmed.substring(1, trimmed.length() - 1).trim();
    }
    return trimmed;
  }

  public boolean isUuid(String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Generates a list with field name, translation code, and default existence for specified dto.
   * Includes inherited fields
   *
   * @param dtoClass whose fields we need.
   * @return the list with the triplet of data.
   */
  public List<FieldDetail> getAnnotatedFieldLabel(Class<?> dtoClass) {
    List<Field> uiRefFields = FieldUtils.getAllFieldsList(dtoClass).stream()
        .filter(f -> ArrayUtils.isNotEmpty(f.getDeclaredAnnotationsByType(UIRef.class))).toList();

    if (CollectionUtils.isEmpty(uiRefFields)) {
      return new ArrayList<>();
    }
    return uiRefFields.stream().map(
        f -> new FieldDetail(f.getName(), f.getDeclaredAnnotation(UIRef.class).text(),
            f.getDeclaredAnnotation(UIRef.class).defaultField(),
            f.getDeclaredAnnotation(UIRef.class).exportOrder())).toList();
  }

  /**
   * Returns the field name of a QueryDSL path (e.g. {@code ingredient.createdBy} -&gt;
   * {@code "createdBy"}).
   *
   * @param path The QueryDSL path.
   * @return The field name.
   */
  public String fieldName(Path<?> path) {
    return path.getMetadata().getName();
  }

  public void executeAfterCommit(Runnable runnable) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      runnable.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

      @Override
      public void afterCommit() {
        runnable.run();
      }
    });
  }

  /**
   * Finds elements in {@code values} whose value for {@code field} already occurred earlier in the
   * list.
   *
   * @param <V>    The element type.
   * @param values The elements to check for duplicates.
   * @param field  The DSL path identifying which field's value determines duplication.
   * @return The elements considered duplicates (every occurrence after the first per field value).
   */
  public <V> Set<V> duplicates(List<V> values, Path<?> field) {
    String propertyName = fieldName(field);
    Set<Object> seen = new HashSet<>();
    return values.stream().filter(value -> !seen.add(findPropertyValueInPath(value, propertyName)))
        .collect(Collectors.toSet());
  }
}
