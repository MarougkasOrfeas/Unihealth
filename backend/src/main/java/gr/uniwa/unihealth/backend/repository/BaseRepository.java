package gr.uniwa.unihealth.backend.repository;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.model.config.EntityFilterConfig;
import gr.uniwa.unihealth.backend.model.enums.FilterMode;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.NoRepositoryBean;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The core Repository class for all entities of the application.
 *
 * @author omaro
 */
@NoRepositoryBean
public interface BaseRepository<E extends BaseEntity>
    extends ExtendedJpaRepository<E, String>, QuerydslPredicateExecutor<E> {

  /**
   * Customizes application bindings.
   *
   * @param bindings     The existing bindings.
   * @param decodeUrl    If a string is a URL and thus need to be decoded.
   * @param filterConfig The filter configuration to use for filtering.
   */
  static void customizeBindings(QuerydslBindings bindings, boolean decodeUrl,
      EntityFilterConfig<?> filterConfig) {
    bindings.bind(String.class).all((path, values) -> {
      if (values.isEmpty()) {
        return Optional.empty();
      }

      List<? extends String> list = values.stream().toList();

      // Collapse multiple values into a single IN instead of an OR chain. On paths that navigate
      // a collection (e.g. "domains.key") Querydsl turns every any() occurrence into its own
      // correlated EXISTS subquery, so an OR chain would emit one subquery per selected value.
      if (resolveFilterMode(path, filterConfig) == FilterMode.EQUALS_IGNORE_CASE) {
        // The column is folded by the database, the values by the JVM. These agree for ASCII;
        // locale-sensitive characters may fold differently than the database collation would.
        Set<String> lowerCasedValues = new LinkedHashSet<>();
        for (String value : list) {
          lowerCasedValues.add(decode(value, decodeUrl).toLowerCase(Locale.ROOT));
        }

        return Optional.of(((StringPath) path).lower().in(lowerCasedValues));
      }

      BooleanExpression result = searchExpression(path, list.get(0), decodeUrl, filterConfig);
      for (String value : list.subList(1, list.size())) {
        result = result.or(searchExpression(path, value, decodeUrl, filterConfig));
      }
      return Optional.of(result);
    });

    bindings.bind(BigDecimal.class).all(BaseRepository::numberBinding);
    bindings.bind(Integer.class).all(BaseRepository::numberBinding);
  }

  /**
   * Expression used in searching.
   *
   * @param path         The path to search to.
   * @param value        The value to search.
   * @param decodeUrl    If the value should pass from url decoding.
   * @param filterConfig The filter configuration to use for filtering.
   * @return The {@link BooleanExpression} that needs to be evaluated.
   */
  static BooleanExpression searchExpression(Path<String> path, String value, boolean decodeUrl,
      EntityFilterConfig<?> filterConfig) {
    String valueToUse = decode(value, decodeUrl);

    return switch (resolveFilterMode(path, filterConfig)) {
      case EQUALS_IGNORE_CASE -> ((StringPath) path).equalsIgnoreCase(valueToUse);
      case CONTAINS_IGNORE_CASE -> ((StringPath) path).containsIgnoreCase(valueToUse);
    };

  }

  /**
   * Resolves the {@link FilterMode} configured for the field the given path points to.
   *
   * @param path         The path being filtered on.
   * @param filterConfig The filter configuration to use, may be {@code null}.
   * @return The configured mode, or {@link FilterMode#EQUALS_IGNORE_CASE} when none applies.
   */
  static FilterMode resolveFilterMode(Path<String> path, EntityFilterConfig<?> filterConfig) {
    if (filterConfig == null) {
      return FilterMode.EQUALS_IGNORE_CASE;
    }

    String fieldName = path.getMetadata().getName();
    return filterConfig.filterModeMap().getOrDefault(fieldName, filterConfig.defaultStringMode());
  }

  /**
   * URL-decodes a filter value, falling back to the raw value when it is not properly encoded.
   *
   * @param value     The value to decode.
   * @param decodeUrl If the value should pass from url decoding.
   * @return The decoded value.
   */
  static String decode(String value, boolean decodeUrl) {
    if (!decodeUrl) {
      return value;
    }

    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      // ignore exception
      return value;
    }
  }

  /**
   * The binding to use for numbers.
   *
   * @param path   The path to search to.
   * @param values The values to search.
   * @return The created {@link Predicate}.
   */
  static <N extends Number & Comparable<?>> Optional<Predicate> numberBinding(Path<N> path,
      Collection<? extends N> values) {
    if (values.isEmpty()) {
      return Optional.empty();
    } else {
      List<? extends N> list = values.stream().toList();
      BooleanExpression result = searchExpression(path, list.get(0));
      for (N value : list.subList(1, list.size())) {
        result = result.or(searchExpression(path, value));
      }
      return Optional.of(result);
    }
  }

  /**
   * Expression used in searching for numbers.
   *
   * @param path  The path to search to.
   * @param value The value to search.
   * @return The {@link BooleanExpression} that needs to be evaluated.
   */
  static <N extends Number & Comparable<?>> BooleanExpression searchExpression(Path<N> path,
      N value) {
    return ((NumberPath<N>) path).stringValue().startsWithIgnoreCase(value.toString());
  }
}
