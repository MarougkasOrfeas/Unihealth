package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.dto.FacetDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.service.BaseReaderService;
import gr.uniwa.unihealth.backend.service.BaseReaderWithSearchService;
import gr.uniwa.unihealth.backend.service.TranslationReaderService;
import gr.uniwa.unihealth.backend.service.util.QueryUtils;
import gr.uniwa.unihealth.backend.service.util.ServiceUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation for {@link BaseReaderService}.
 *
 * @author omaro
 */
@Transactional
public abstract class BaseReaderServiceImpl<D extends BaseDTO, E extends BaseEntity>
    implements BaseReaderService<D> {

  @Autowired
  protected QueryUtils queryUtils;

  @Autowired
  private TranslationReaderService translationReaderService;

  @Override
  public Optional<D> findOptionalById(String id) {
    return findOptionalEntityById(id).map(this::mapToDTOForSingleRead);
  }

  @Override
  public D findById(String id) {
    return findOptionalById(id).orElseThrow(QDoesNotExistException::new);
  }

  @Override
  public Page<D> findAll(Map<String, Object> parameters) {
    Map<String, Object> parametersToUse = new HashMap<>();

    if (parameters != null) {
      parametersToUse.putAll(parameters);
    }

    Predicate predicate = createPredicateFromParams(parametersToUse);
    Pageable pageable = queryUtils.getPageable(parametersToUse);
    return findAll(predicate, pageable);
  }

  private Predicate createPredicateFromParams(Map<String, Object> parameters) {
    String search =
        ServiceUtils.stripEnclosingQuotes(Objects.toString(parameters.remove("search"), ""));
    String locale = Objects.toString(parameters.remove("locale"), "de").trim();
    String searchStrategy = Objects.toString(parameters.remove("searchStrategy"), "").trim();
    boolean pickerMode =
        Boolean.parseBoolean(Objects.toString(parameters.remove("pickerMode"), "false").trim());

    Predicate predicate = queryUtils.getPredicate(parameters,
        ServiceUtils.genericType(this, BaseReaderServiceImpl.class, 1));

    if (this instanceof BaseReaderWithSearchService<?> readerWithSearchService) {
      if (StringUtils.isNotBlank(search)) {
        predicate = ExpressionUtils.allOf(predicate, readerWithSearchService.buildSearchPredicate(
            new BaseReaderWithSearchService.BuildSearchPredicateParams(search, locale,
                searchStrategy, pickerMode)));
      }
    }
    return predicate;
  }

  protected boolean translatedLabelMatches(String labelCode, String locale, String search) {
    String label = translationReaderService.translate(labelCode, locale);
    if (StringUtils.isBlank(label) || StringUtils.isBlank(search)) {
      return false;
    }

    String normalizedLabel = label.toLowerCase();
    String normalizedSearch = search.toLowerCase();
    return normalizedLabel.contains(normalizedSearch);
  }

  @Override
  public Page<D> findAll(Predicate predicate, Pageable pageable) {
    return getRepository().findPage(predicate, withSortTiebreaker(pageable))
        .map(this::mapToDTOForListRead);
  }

  private Pageable withSortTiebreaker(Pageable pageable) {
    Sort sort = pageable.getSort();
    for (Sort.Order tiebreaker : sortTiebreaker()) {
      if (sort.getOrderFor(tiebreaker.getProperty()) == null) {
        sort = sort.and(Sort.by(tiebreaker));
      }
    }

    return sort.equals(pageable.getSort()) ?
        pageable :
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
  }

  protected Sort sortTiebreaker() {
    return Sort.by(Sort.Order.asc("id"));
  }

  @Override
  public List<D> findAll() {
    return getRepository().findAll().stream().map(this::mapToDTOForListRead).toList();
  }

  @Override
  public Map<String, D> findByIds(Collection<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Map.of();
    }
    Set<String> distinctIds =
        ids.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    if (distinctIds.isEmpty()) {
      return Map.of();
    }
    return getRepository().findAllById(distinctIds).stream().map(this::mapToDTOForListRead)
        .collect(Collectors.toMap(D::getId, Function.identity()));
  }

  protected E findEntityById(String id) {
    return findEntityById(id, false);
  }

  protected E findEntityById(String id, boolean refresh) {
    final E entity = findOptionalEntityById(id).orElseThrow(QDoesNotExistException::new);
    if (refresh) {
      getRepository().flushAndRefresh(entity);
    }
    return entity;
  }

  private Optional<E> findOptionalEntityById(String id) {
    return getRepository().findById(id);
  }

  protected D mapToDTOForSingleRead(E entity) {
    return getMapper().mapToDTO(entity);
  }

  protected D mapToDTOForListRead(E entity) {
    return getMapper().mapToDTO(entity);
  }

  @Override
  public List<String> loadFacetOptions(FacetDTO facetParams) {
    Predicate predicate = createFacetPredicate(facetParams);
    return getRepository().loadFacetOptions(facetParams, predicate);
  }

  private Predicate createFacetPredicate(FacetDTO facetParams) {
    Predicate predicate = queryUtils.getPredicate(facetParams.getPredicateParams(),
        ServiceUtils.genericType(this, BaseReaderServiceImpl.class, 1));

    String search = ServiceUtils.stripEnclosingQuotes(facetParams.getSearch());

    if (StringUtils.isNotBlank(
        search) && (this instanceof BaseReaderWithSearchService<?> readerWithSearchService)) {
      BooleanExpression searchPredicate = readerWithSearchService.buildSearchPredicate(
          new BaseReaderWithSearchService.BuildSearchPredicateParams(search,
              facetParams.getLocale(), StringUtils.trim(facetParams.getSearchStrategy()),
              facetParams.isPickerMode()));
      if (searchPredicate != null) {
        predicate = ExpressionUtils.allOf(predicate, searchPredicate);
      }
    }
    return predicate;
  }

  protected abstract BaseEntityMapper<D, E> getMapper();

  protected abstract BaseRepository<E> getRepository();
}
