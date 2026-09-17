package gr.uniwa.unihealth.backend.service.util;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.*;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.model.config.EntityFilterConfig;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QueryUtils {

  private static final String NOT_SUFFIX = ".not";

  private final ApplicationContext applicationContext;
  private final ObjectProvider<ConversionService> conversionService;
  private final EntityFilterConfigRegistry entityFilterConfigRegistry;

  private QuerydslPredicateBuilder querydslPredicateBuilder;
  private QuerydslBindingsFactory querydslBindingsFactory;

  @PostConstruct
  public void init() {
    querydslBindingsFactory = applicationContext.getBean(QuerydslBindingsFactory.class);

    querydslPredicateBuilder = new QuerydslPredicateBuilder(
        conversionService.getIfUnique(DefaultConversionService::getSharedInstance),
        querydslBindingsFactory.getEntityPathResolver());
  }

  /**
   * Converts the given parameters to a Pageable object. The parameters are expected to contain
   * "page" and "size" keys for pagination, and optionally a "sort" key for sorting.
   *
   * @param params The parameters containing pagination and sorting information.
   * @return A Pageable object representing the pagination and sorting information.
   */
  public Pageable getPageable(Map<String, Object> params) {
    Pageable pageable;
    if (params.containsKey("page") && params.containsKey("size")) {
      @SuppressWarnings("unchecked")
      Sort sort = ((Collection<String>) params.getOrDefault("sort", List.of())).stream()
          .filter(s -> !StringUtils.isBlank(s)).map(s -> s.split(",")).map(sortArray -> Sort.by(
              new Sort.Order(Direction.fromString(sortArray[1].trim()),
                  sortArray[0].trim()).ignoreCase())).reduce(Sort.unsorted(), Sort::and);

      pageable = PageRequest.of(Integer.parseInt(params.get("page").toString()),
          Integer.parseInt(params.get("size").toString()), sort);
    } else {
      pageable = Pageable.ofSize(10);
    }

    return pageable;
  }

  /**
   * Converts the given parameters to a QueryDSL {@link Predicate} for the given entity type. The
   * keys of the parameters are expected to be in the format "attributePath[.not]", where
   * "attributePath" is the path to the attribute to filter by (e.g. "name" or "address.city") and
   * the optional ".not" suffix indicates that the predicate should be negated. The values of the
   * parameters are expected to be either a single value or a collection of values to filter by. For
   * date and date-time attributes, the values can also be in the format "yyyy-MM-dd" for exact date
   * matching or "yyyy-MM-ddTHH:mm:ss.SSS-yyyy-MM-ddTHH:mm:ss.SSS" for date-time range matching.
   *
   * @param <E>    the type of the entity
   * @param params the parameters to convert
   * @param type   the type of the entity
   * @return the resulting Predicate
   */
  public <E extends BaseEntity> Predicate getPredicate(Map<String, Object> params, Class<E> type) {
    Map<String, List<String>> convertedParams = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> convertValue(entry.getValue())));

    Map<String, List<String>> paramsToUse = new HashMap<>();
    List<Predicate> additionalPredicates = new ArrayList<>();

    TypeInformation<E> typeInformation = TypeInformation.of(type);
    QuerydslBindings querydslBindings = querydslBindingsFactory.createBindingsFor(typeInformation);
    EntityFilterConfig<E> filterConfig = entityFilterConfigRegistry.getConfig(type);
    BaseRepository.customizeBindings(querydslBindings, false, filterConfig);

    for (Entry<String, List<String>> entry : convertedParams.entrySet()) {
      try {
        String attributePath = entry.getKey();
        boolean not = false;

        if (attributePath.endsWith(NOT_SUFFIX)) {
          attributePath = attributePath.substring(0, attributePath.length() - NOT_SUFFIX.length());
          not = true;
        }

        List<String> filterValues = entry.getValue();
        Expression<?> expression =
            ServiceUtils.findExpressionInPath(SimpleEntityPathResolver.INSTANCE.createPath(type),
                attributePath);

        if (expression instanceof DateTimePath<?> dateTimePath) {
          @SuppressWarnings("unchecked")
          DateTimePath<LocalDateTime> localDateTimePath =
              (DateTimePath<LocalDateTime>) dateTimePath;

          BooleanExpression dateTimePredicate = anyOf(filterValues.stream().map(filterValue -> {
            Matcher rangeMatcher = DateUtils.DATE_TIME_RANGE_FILTER_PATTERN.matcher(filterValue);
            if (rangeMatcher.matches()) {
              return localDateTimePath.between(LocalDateTime.parse(rangeMatcher.group(1)),
                  LocalDateTime.parse(rangeMatcher.group(2)));
            }

            LocalDate date = DateUtils.parseSearchDate(filterValue);
            return date == null ? null : matchesDay(localDateTimePath, date);
          }).toList());

          addNegatable(additionalPredicates, dateTimePredicate, not);
        } else if (expression instanceof CollectionPathBase<?, ?, ?> collectionPath && collectionPath.any() instanceof EnumPath<?> enumPath) {

          BooleanExpression collectionPredicate = anyOf(filterValues.stream()
              .map(filterValue -> enumPath.stringValue().containsIgnoreCase(filterValue)).toList());

          addNegatable(additionalPredicates, collectionPredicate, not);
        } else if (expression instanceof CollectionPathBase<?, ?, ?> collectionPath && collectionPath.any() instanceof StringPath elementPath) {

          // Filtering a collection of strings means filtering its elements. The generic binding
          // cannot do it: Spring Data hands a collection-valued property the whole filter value
          // list wrapped into a single element. One IN keeps it to a single correlated EXISTS,
          // see the note in BaseRepository#customizeBindings.
          BooleanExpression collectionPredicate = elementPath.lower()
              .in(filterValues.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList());

          addNegatable(additionalPredicates, collectionPredicate, not);
        } else if (not) {
          MultiValueMap<String, String> singletonValueMap =
              new MultiValueMapAdapter<>(Map.of(attributePath, filterValues));
          Predicate notPredicate =
              querydslPredicateBuilder.getPredicate(typeInformation, singletonValueMap,
                  querydslBindings).not();
          additionalPredicates.add(notPredicate);
        } else {
          paramsToUse.put(attributePath, filterValues);
        }
      } catch (IllegalArgumentException e) {
        if (!(e.getCause() instanceof NoSuchFieldException)) {
          throw e;
        }
      }
    }

    MultiValueMap<String, String> multiValueMap = new MultiValueMapAdapter<>(paramsToUse);
    Predicate predicateToUse =
        querydslPredicateBuilder.getPredicate(typeInformation, multiValueMap, querydslBindings);

    for (Predicate additionalPredicate : additionalPredicates) {
      predicateToUse = ExpressionUtils.allOf(predicateToUse, additionalPredicate);
    }

    return predicateToUse;
  }

  private List<String> convertValue(Object value) {
    List<String> result;
    if (value instanceof Collection<?> collectionValue) {
      result = collectionValue.stream().filter(Objects::nonNull).map(Object::toString).toList();
    } else if (value != null) {
      result = List.of(value.toString());
    } else {
      result = List.of();
    }

    return result.stream().filter(v -> !StringUtils.isBlank(v)).map(String::trim).toList();
  }

  private static BooleanExpression matchesDay(DateTimePath<LocalDateTime> path, LocalDate date) {
    return path.between(date.atStartOfDay(), date.atTime(LocalTime.MAX));
  }

  private static void addNegatable(List<Predicate> target, BooleanExpression expression,
      boolean not) {
    if (expression != null) {
      target.add(not ? expression.not() : expression);
    }
  }

  /**
   * Builds a combined OR predicate for searching a value across a set of columns, leaving out the
   * columns the caller handles with a hand-written predicate.
   *
   * @param columns         The columns to search in.
   * @param search          The search string.
   * @param excludedColumns The columns to leave out of the generic search.
   * @return A BooleanExpression combining all supported field predicates with OR, or null if none
   * of the fields produced a predicate.
   */
  public BooleanExpression buildFieldSearchPredicate(List<SimpleExpression<?>> columns,
      String search, Collection<? extends Expression<?>> excludedColumns) {
    return buildFieldSearchPredicate(
        columns.stream().filter(col -> !excludedColumns.contains(col)).toList(), search);
  }

  /**
   * Builds a combined OR predicate for searching a value across a set of columns.
   *
   * @param columns The columns to search in.
   * @param search  The search string.
   * @return A BooleanExpression combining all supported field predicates with OR, or null if none
   * of the fields produced a predicate.
   */
  public BooleanExpression buildFieldSearchPredicate(List<SimpleExpression<?>> columns,
      String search) {
    return anyOf(
        columns.stream().map(column -> buildFieldSearchPredicate(column, search)).toList());
  }

  private BooleanExpression buildFieldSearchPredicate(Expression<?> expression, String search) {
    return switch (expression) {
      case StringExpression stringExpression -> stringExpression.containsIgnoreCase(search);
      case EnumPath<?> enumPath -> enumPath.stringValue().containsIgnoreCase(search);
      case NumberPath<?> numberPath -> numberPredicate(numberPath, search);
      // Searching a collection column means searching its elements.
      case CollectionPathBase<?, ?, ?> collectionPath ->
          buildFieldSearchPredicate(collectionPath.any(), search);
      case DatePath<?> datePath -> datePredicate(datePath, search);
      case DateTimePath<?> dateTimePath -> dateTimePredicate(dateTimePath, search);
      case null, default -> null; // unsupported field type — caller should skip it
    };
  }

  private BooleanExpression numberPredicate(NumberPath<?> path, String search) {
    if (BigDecimal.class.isAssignableFrom(path.getType())) {
      @SuppressWarnings("unchecked")
      NumberPath<BigDecimal> decimalPath = (NumberPath<BigDecimal>) path;
      return buildDecimalPredicate(search, decimalPath);
    }

    // Avoid CAST operations for text searches.
    return search.matches(".*\\d.*") ? path.stringValue().containsIgnoreCase(search) : null;
  }

  private BooleanExpression datePredicate(DatePath<?> path, String search) {
    LocalDate parsedDate = DateUtils.parseSearchDate(search);
    if (parsedDate == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    DatePath<LocalDate> localDatePath = (DatePath<LocalDate>) path;
    return localDatePath.eq(parsedDate);
  }

  private BooleanExpression dateTimePredicate(DateTimePath<?> path, String search) {
    LocalDate parsedDate = DateUtils.parseSearchDate(search);
    if (parsedDate == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    DateTimePath<LocalDateTime> localDateTimePath = (DateTimePath<LocalDateTime>) path;
    return matchesDay(localDateTimePath, parsedDate);
  }

  private BooleanExpression buildDecimalPredicate(String search, NumberPath<BigDecimal> path) {
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
   * Varargs variant of {@link #anyOfOrNoMatch(List)}, for the callers that build a fixed set of
   * search predicates rather than collecting them in a list.
   *
   * @param predicates The search predicates to combine, individual entries may be null.
   * @return The combined expression, or an always-false expression if none of them was usable.
   */
  public BooleanExpression anyOfOrNoMatch(BooleanExpression... predicates) {
    return anyOfOrNoMatch(Arrays.asList(predicates));
  }

  /**
   * Combines the given search predicates with OR, falling back to an always-false expression when
   * none of them could be built. This is the terminal call of every implementation: a search the
   * visible columns cannot be matched against must yield an empty result, not the whole table.
   *
   * @param predicates The search predicates to combine, individual entries may be null.
   * @return The combined expression, or an always-false expression if none of them was usable.
   */
  public BooleanExpression anyOfOrNoMatch(List<BooleanExpression> predicates) {
    BooleanExpression combined = anyOf(predicates);
    // "1 = 2" is the always-false predicate: it matches no row without referencing any column.
    return combined != null ? combined : Expressions.ONE.eq(Expressions.TWO);
  }

  /**
   * Combines the given expressions with OR, skipping the ones that could not be built.
   *
   * @param expressions The expressions to combine.
   * @return The combined expression, or null if none of them was usable.
   */
  public BooleanExpression anyOf(List<BooleanExpression> expressions) {
    return expressions.stream().filter(Objects::nonNull).reduce(BooleanExpression::or).orElse(null);
  }
}
