package gr.uniwa.unihealth.backend.controller.util;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.service.util.ServiceUtils;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Component that contains controller utils.
 *
 * @author European Dynamics SA
 */
@Component
@RequiredArgsConstructor
public class ControllerUtils {

  private static final Pattern DATE_TIME_FILTER_PATTERN = Pattern.compile(
      "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})-(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})$");

  private final ApplicationContext applicationContext;
  private final ObjectProvider<ConversionService> conversionService;

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
   * Gets a predicate and the pagination information from the body of the request.
   *
   * @param requestBody The body of the request.
   * @param type        The type of the predicate.
   * @return The predicate and the pagination information.
   */
  public <E extends BaseEntity> Entry<Predicate, Pageable> getPredicateAndPageable(
      Map<String, Object> requestBody, Class<E> type) {
    Map<String, Object> params = getParamsFromRequestBody(requestBody);

    Predicate predicate = getPredicate(params, type);

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

    return new AbstractMap.SimpleEntry<>(predicate, pageable);
  }

  private <E extends BaseEntity> Predicate getPredicate(Map<String, Object> params, Class<E> type) {
    Map<String, List<String>> convertedParams = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> convertValue(entry.getValue())));

    Map<String, List<String>> paramsToUse = new HashMap<>();
    List<Predicate> additionalPredicates = new ArrayList<>();

    for (Entry<String, List<String>> entry : convertedParams.entrySet()) {
      try {
        String attributePath = entry.getKey();
        List<String> filterValues = entry.getValue();
        Expression<?> expression =
            ServiceUtils.findExpressionInPath(SimpleEntityPathResolver.INSTANCE.createPath(type),
                attributePath);

        if (expression instanceof DateTimePath<?> dateTimePath && filterValues.size() == 1) {
          String filterValue = filterValues.get(0);
          Matcher matcher = DATE_TIME_FILTER_PATTERN.matcher(filterValue);
          if (matcher.matches()) {
            LocalDateTime fromDateTime = LocalDateTime.parse(matcher.group(1));
            LocalDateTime toDateTime = LocalDateTime.parse(matcher.group(2));

            @SuppressWarnings("unchecked")
            BooleanExpression betweenPredicate =
                ((DateTimePath<LocalDateTime>) dateTimePath).between(fromDateTime, toDateTime);

            additionalPredicates.add(betweenPredicate);
          }
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
    TypeInformation<E> typeInformation = TypeInformation.of(type);

    QuerydslBindings querydslBindings = querydslBindingsFactory.createBindingsFor(typeInformation);
    BaseRepository.customizeBindings(querydslBindings, false);

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
      result = Collections.singletonList(value.toString());
    } else {
      result = Collections.emptyList();
    }

    return result.stream().filter(v -> !StringUtils.isBlank(v)).map(String::trim).toList();
  }

  private Map<String, Object> getParamsFromRequestBody(Map<String, Object> requestBody) {
    return requestBody != null ? requestBody : Map.of();
  }
}
