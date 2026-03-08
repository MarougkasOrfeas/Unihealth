package gr.uniwa.unihealth.backend.repository;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.NoRepositoryBean;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
   * @param bindings  The existing bindings.
   * @param decodeUrl If a string is a URL and thus need to be decoded.
   */
  static void customizeBindings(QuerydslBindings bindings, boolean decodeUrl) {
    bindings.bind(String.class).all((path, values) -> {
      if (values.isEmpty()) {
        return Optional.empty();
      } else {
        List<? extends String> list = values.stream().toList();
        BooleanExpression result = searchExpression(path, list.get(0), decodeUrl);
        for (String value : list.subList(1, list.size())) {
          result = result.or(searchExpression(path, value, decodeUrl));
        }
        return Optional.of(result);
      }
    });

    bindings.bind(BigDecimal.class).all(BaseRepository::numberBinding);
    bindings.bind(Integer.class).all(BaseRepository::numberBinding);
  }

  /**
   * Expression used in searching.
   *
   * @param path      The path to search to.
   * @param value     The value to search.
   * @param decodeUrl If the value should pass from url decoding.
   * @return The {@link BooleanExpression} that needs to be evaluated.
   */
  static BooleanExpression searchExpression(Path<String> path, String value, boolean decodeUrl) {
    String valueToUse = value;
    try {
      valueToUse = decodeUrl ? URLDecoder.decode(value, StandardCharsets.UTF_8) : valueToUse;
    } catch (IllegalArgumentException e) {
      // ignore exception
    }

    // Use exact match (equals) instead of partial match (startsWith)
    // This ensures filters return only exact matches
    return ((StringPath) path).equalsIgnoreCase(valueToUse);
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
