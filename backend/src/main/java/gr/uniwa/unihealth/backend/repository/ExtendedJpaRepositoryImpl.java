package gr.uniwa.unihealth.backend.repository;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.lexicon.model.QData;
import com.eurodyn.qlack.fuse.lexicon.model.QKey;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.AbstractJPAQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import gr.uniwa.unihealth.backend.dto.FacetDTO;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.service.util.DateUtils;
import gr.uniwa.unihealth.backend.service.util.ServiceUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.*;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation for {@link ExtendedJpaRepository}.
 *
 * @author omaro
 */
public class ExtendedJpaRepositoryImpl<T, I> extends SimpleJpaRepository<T, I>
    implements ExtendedJpaRepository<T, I> {

  private static final Pattern translate_pattern = Pattern.compile("^(.*)\\.translate\\.(..)$");

  private final EntityManager entityManager;
  private final EntityPath<T> path;
  private final Querydsl querydsl;

  /**
   * Creates a new {@link ExtendedJpaRepositoryImpl} to manage objects of the given
   * {@link JpaEntityInformation}.
   *
   * @param jpaEntityInformation must not be {@literal null}.
   * @param entityManager        must not be {@literal null}.
   */
  public ExtendedJpaRepositoryImpl(JpaEntityInformation<T, I> jpaEntityInformation,
      EntityManager entityManager) {
    super(jpaEntityInformation, entityManager);
    this.entityManager = entityManager;
    this.path = SimpleEntityPathResolver.INSTANCE.createPath(jpaEntityInformation.getJavaType());
    this.querydsl =
        new Querydsl(entityManager, new PathBuilder<>(path.getType(), path.getMetadata()));
  }

  @Override
  public void flushAndRefresh(T entity) {
    entityManager.flush();
    entityManager.refresh(entity);
  }

  @Override
  public Optional<T> findByIdForUpdate(I id) {
    return Optional.ofNullable(
        entityManager.find(getDomainClass(), id, LockModeType.PESSIMISTIC_WRITE));
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Page<T> findPage(Predicate predicate, Pageable pageable) {
    Assert.notNull(predicate, "Predicate must not be null!");
    Assert.notNull(pageable, "Pageable must not be null!");

    JPQLQuery<?> fetchQuery = createQuery(predicate);
    JPQLQuery<T> query = fetchQuery.select(path);

    Predicate predicateToUse = predicate;
    int keyCount = 0;
    if (!pageable.getSort().isUnsorted()) {
      for (Sort.Order order : pageable.getSort()) {
        Matcher matcher = translate_pattern.matcher(order.getProperty());
        if (matcher.find()) {
          String property = matcher.group(1);
          String locale = matcher.group(2);

          QKey key = new QKey("k" + ++keyCount);
          QData data = new QData("d" + ++keyCount);

          com.querydsl.core.types.Order direction = order.isAscending() ?
              com.querydsl.core.types.Order.ASC :
              com.querydsl.core.types.Order.DESC;

          Expression<?> expression = ServiceUtils.findExpressionInPath(path, property);
          if (expression instanceof CollectionPathBase<?, ?, ?> collectionPath) {
            PathBuilder<?> element = new PathBuilder<>(collectionPath.any().getType(),
                property.replace('.', '_') + "Element" + keyCount);

            query = joinCollection(query, collectionPath, element).leftJoin(key)
                .on(key.name.eq(translationKeyExpression(property, element))).leftJoin(data)
                .on(data.key.id.eq(key.id).and(data.language.locale.eq(locale))).groupBy(path)
                .orderBy(new OrderSpecifier(direction, Expressions.stringTemplate(
                    "listagg(coalesce({0}, cast({1} as String)), ', ') within group (order by coalesce({0}, cast({1} as String)))",
                    data.value, element)));
          } else {
            query.join(key).on(key.name.eq(translationKeyExpression(property, expression)))
                .join(data).on(data.key.id.eq(key.id))
                .orderBy(new OrderSpecifier(direction, data.value));
            predicateToUse = data.language.locale.eq(locale).and(predicateToUse);
          }
          continue;
        }

        // Reached only for a plain property: a `.translate.<locale>` one has no such field on the
        // entity, so resolving it as a path would throw.
        Expression exp = ServiceUtils.findExpressionInPath(path, order.getProperty());
        boolean isDateField =
            LocalDate.class.equals(exp.getType()) || LocalDateTime.class.equals(exp.getType());
        if (exp instanceof CollectionPathBase<?, ?, ?> collectionPath) {
          // Sort collection fields by one combined text value, e.g. ["AF", "SZ"] -> "AF, SZ".
          com.querydsl.core.types.Order direction = order.isAscending() ?
              com.querydsl.core.types.Order.ASC :
              com.querydsl.core.types.Order.DESC;

          PathBuilder<?> element = new PathBuilder<>(collectionPath.any().getType(),
              order.getProperty().replace('.', '_') + "Element" + ++keyCount);

          query = joinCollection(query, collectionPath, element).groupBy(path)
              .orderBy(new OrderSpecifier(direction, collectionSortExpression(element)));
        } else if (isDateField) {
          com.querydsl.core.types.Order direction = order.isAscending() ?
              com.querydsl.core.types.Order.ASC :
              com.querydsl.core.types.Order.DESC;

          Expression<java.time.LocalDate> dateOnly =
              Expressions.dateTemplate(java.time.LocalDate.class, "cast({0} as date)", exp);

          query = query.orderBy(new OrderSpecifier<>(direction, dateOnly));
        } else if (String.class.equals(exp.getType())) {
          query = query.orderBy(blankAsNullOrder(order, exp));
        } else {
          query = querydsl.applySorting(Sort.by(order), query);
        }
      }
    }

    query.where(predicateToUse);

    JPQLQuery<?> countQuery = createCountQuery(predicate);
    final JPQLQuery<T> pagedQuery =
        querydsl.applyPagination(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
            query);

    return PageableExecutionUtils.getPage(pagedQuery.fetch(), pageable, countQuery::fetchCount);
  }

  private JPQLQuery<?> createCountQuery(Predicate predicate) {
    return doCreateQuery(getQueryHintsForCount(), predicate);
  }

  private AbstractJPAQuery<?, ?> createQuery(Predicate predicate) {
    AbstractJPAQuery<?, ?> query =
        doCreateQuery(getQueryHints().withFetchGraphs(entityManager), predicate);
    CrudMethodMetadata metadata = getRepositoryMethodMetadata();
    if (metadata == null) {
      return query;
    }

    LockModeType type = metadata.getLockModeType();
    return type == null ? query : query.setLockMode(type);
  }

  private AbstractJPAQuery<?, ?> doCreateQuery(QueryHints hints, Predicate predicate) {
    AbstractJPAQuery<?, ?> query = querydsl.createQuery(path);

    if (predicate != null) {
      query = query.where(predicate);
    }

    hints.forEach(query::setHint);

    return query;
  }

  /**
   * Sorts a string column on {@code nullif(trim(col), '')}, so blank or whitespace-only values are
   * ordered like NULL.
   */
  private OrderSpecifier<String> blankAsNullOrder(Sort.Order order, Expression<?> expression) {
    com.querydsl.core.types.Order direction = order.isAscending() ?
        com.querydsl.core.types.Order.ASC :
        com.querydsl.core.types.Order.DESC;

    StringExpression sortExpression =
        Expressions.stringTemplate("nullif(trim({0}), '')", expression);
    if (order.isIgnoreCase()) {
      sortExpression = sortExpression.lower();
    }

    return new OrderSpecifier<>(direction, sortExpression);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private JPQLQuery<T> joinCollection(JPQLQuery<T> query, CollectionPathBase<?, ?, ?> collection,
      Path<?> alias) {
    return query.leftJoin((CollectionPathBase) collection, (Path) alias);
  }

  private StringExpression translationKeyExpression(String property, Expression<?> value) {
    return Expressions.stringTemplate("concat('" + translationPrefix(property) + "', {0})", value);
  }

  private StringExpression collectionSortExpression(Expression<?> value) {
    // SQL expression for sorting collection fields.
    return Expressions.stringTemplate(
        "listagg(cast({0} as String), ', ') within group (order by cast({0} as String))", value);
  }

  private String translationPrefix(String property) {
    if ("permissions".equalsIgnoreCase(property)) {
      return "permission.";
    }
    // Terminart is a hardcoded enum stored in English, so sorting has to run on the label.
    if ("eventType".equalsIgnoreCase(property)) {
      return "planning.events.type.";
    }

    return "";
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<String> loadFacetOptions(FacetDTO facetParams, Predicate predicate) {
    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
    JPAQuery<?> untypedQuery = queryFactory.from(path);

    EntityPath currentEntityPath = path;
    String currentPropertyPath = facetParams.getColumn();
    while (currentPropertyPath.contains(".")) {
      int dotIndex = currentPropertyPath.indexOf(".");

      String firstPath = currentPropertyPath.substring(0, dotIndex);
      Expression<?> firstExpression =
          ServiceUtils.findPropertyExpressionOfPath(currentEntityPath, firstPath);

      currentPropertyPath = currentPropertyPath.substring(dotIndex + 1);

      if (firstExpression instanceof EntityPath firstExpressionAsEntityPath) {
        currentEntityPath = createPathWithAlias(firstExpressionAsEntityPath.getType(), firstPath);
        untypedQuery.join(firstExpressionAsEntityPath, currentEntityPath);
      } else if (firstExpression instanceof CollectionPathBase<?, ?, ?> firstExpressionAsCollectionPath) {
        currentEntityPath =
            createPathWithAlias(firstExpressionAsCollectionPath.any().getType(), firstPath);
        untypedQuery.join(firstExpressionAsCollectionPath, currentEntityPath);
      } else {
        throw ExceptionUtils.createException(QCouldNotSaveException.class,
            "unsupported_expression_path", "Unsupported expression path: {}",
            facetParams.getColumn());
      }
    }

    Expression<?> rawExpressionToSelect =
        ServiceUtils.findExpressionInPath(currentEntityPath, currentPropertyPath);
    boolean sortable = !(rawExpressionToSelect instanceof CollectionPathBase<?, ?, ?>);
    StringExpression stringExpressionToSelect;
    StringExpression filterExpression = null;
    if (rawExpressionToSelect instanceof StringExpression stringExpression) {
      stringExpressionToSelect = stringExpression;
    } else if (LocalDate.class.equals(
        rawExpressionToSelect.getType()) || LocalDateTime.class.equals(
        rawExpressionToSelect.getType())) {
      stringExpressionToSelect =
          Expressions.stringTemplate("to_char({0}, '" + DateUtils.TIMESTAMP_FORMATTER + "')",
              rawExpressionToSelect);
    } else if (BigDecimal.class.equals(rawExpressionToSelect.getType())) {
      stringExpressionToSelect = Expressions.stringTemplate(
          "trim(trailing '.' from trim(trailing '0' from CAST({0} as text)))",
          rawExpressionToSelect);
    } else if (rawExpressionToSelect instanceof CollectionPathBase<?, ?, ?> collectionPath) {
      if (StringUtils.isNotBlank(translationPrefix(currentPropertyPath))) {
        PathBuilder<?> element = new PathBuilder<>(collectionPath.any().getType(),
            currentPropertyPath.replace('.', '_') + "FacetElement");
        stringExpressionToSelect = Expressions.stringTemplate("CAST({0} as text)", element);
        filterExpression =
            translatedCollectionExpression(untypedQuery, collectionPath, currentPropertyPath,
                facetParams.getLocale(), element);
      } else {
        stringExpressionToSelect =
            Expressions.stringTemplate("CAST({0} as text)", collectionPath.any());
      }
    } else {
      stringExpressionToSelect =
          Expressions.stringTemplate("CAST({0} as text)", rawExpressionToSelect);
    }

    JPAQuery<String> query =
        untypedQuery.select(stringExpressionToSelect).distinct().where(predicate)
            .where(stringExpressionToSelect.isNotNull())
            .where(stringExpressionToSelect.isNotEmpty()).limit(100);

    if (StringUtils.isNotBlank(facetParams.getColumnFilter())) {
      StringExpression columnFilterExpression =
          filterExpression != null ? filterExpression : stringExpressionToSelect;
      query.where(columnFilterExpression.containsIgnoreCase(facetParams.getColumnFilter().trim()));
    }

    if (sortable) {
      query.orderBy(stringExpressionToSelect.asc());
    }

    return query.fetch();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private StringExpression translatedCollectionExpression(JPAQuery<?> query,
      CollectionPathBase<?, ?, ?> collectionPath, String property, String locale,
      PathBuilder<?> element) {
    QKey key = new QKey("facetKey");
    QData data = new QData("facetData");

    query.join((CollectionPathBase) collectionPath, (Path) element).leftJoin(key)
        .on(key.name.eq(translationKeyExpression(property, element))).leftJoin(data)
        .on(data.key.id.eq(key.id).and(data.language.locale.eq(locale)));

    return Expressions.stringTemplate("coalesce({0}, cast({1} as String))", data.value, element);
  }

  @SuppressWarnings("unchecked")
  private <E> EntityPath<E> createPathWithAlias(Class<E> entityType, String alias) {
    String qClassName = entityType.getPackageName() + ".Q" + entityType.getSimpleName();
    try {
      Class<?> qClass = Class.forName(qClassName);
      return (EntityPath<E>) qClass.getConstructor(String.class).newInstance(alias);
    } catch (ReflectiveOperationException e) {
      return new PathBuilder<>(entityType, alias);
    }
  }
}

