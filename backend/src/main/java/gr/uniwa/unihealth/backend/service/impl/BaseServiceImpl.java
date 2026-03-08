package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation for {@link BaseService}.
 *
 * @author omaro
 */
@Slf4j
@Transactional
public abstract class BaseServiceImpl<D extends BaseDTO, E extends BaseEntity>
    implements BaseService<D> {

  @Override
  public String create(D dto) {
    validateForCreate(dto);

    E entity = getMapper().mapForCreate(dto);
    entity.setId(null);

    log.info("Creating new {}", entity.getClass().getSimpleName());
    entity = getRepository().save(entity);

    return entity.getId();
  }

  @Override
  public void delete(String id) {
    E entity = findEntityById(id);
    validateForDelete(id, entity);
    log.info("Deleting {} with id {}", entity.getClass().getSimpleName(), entity.getId());
    getRepository().delete(entity);
  }

  protected E findEntityById(String id) {
    return getReaderService().findEntityById(id);
  }

  protected abstract BaseReaderServiceImpl<D, E> getReaderService();

  protected abstract BaseEntityMapper<D, E> getMapper();

  protected abstract BaseRepository<E> getRepository();

  protected void validateForCreate(D dto) {
    validate(null, dto);
  }

  protected void validateForDelete(String id, E entity) {
    // default does nothing
  }

  protected void validate(String id, D dto) {
    // default does nothing
  }
}
