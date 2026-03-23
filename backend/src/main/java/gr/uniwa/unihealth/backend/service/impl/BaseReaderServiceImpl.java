package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.service.BaseReaderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation for {@link BaseReaderService}.
 *
 * @author omaro
 */
@Transactional
public abstract class BaseReaderServiceImpl<D extends BaseDTO, E extends BaseEntity>
    implements BaseReaderService<D> {

  @Override
  public D findById(String id) {
    E entity = findEntityById(id);
    return getMapper().mapToDTO(entity);
  }

  @Override
  public Page<D> findAll(Predicate predicate, Pageable pageable) {
    return getRepository().findAll(predicate, withIdAsSortTiebreaker(pageable))
        .map(getMapper()::mapToDTO);
  }

  private Pageable withIdAsSortTiebreaker(Pageable pageable) {
    Sort sort = pageable.getSort();
    if (sort.getOrderFor("id") != null) {
      return pageable;
    }
    Sort stable =
        sort.isSorted() ? sort.and(Sort.by(Sort.Order.asc("id"))) : Sort.by(Sort.Order.asc("id"));
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stable);
  }

  @Override
  public List<D> findAll() {
    return getRepository().findAll().stream().map(getMapper()::mapToDTO).toList();
  }

  protected E findEntityById(String id) {
    return findEntityById(id, false);
  }

  protected E findEntityById(String id, boolean refresh) {
    final E entity = getRepository().findById(id).orElseThrow(QDoesNotExistException::new);
    if (refresh) {
      getRepository().flushAndRefresh(entity);
    }
    return entity;
  }

  protected abstract BaseEntityMapper<D, E> getMapper();

  protected abstract BaseRepository<E> getRepository();
}
