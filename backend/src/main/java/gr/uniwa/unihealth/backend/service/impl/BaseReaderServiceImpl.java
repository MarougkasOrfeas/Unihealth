package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.service.BaseReaderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    return getRepository().findAll(predicate, pageable).map(getMapper()::mapToDTO);
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
