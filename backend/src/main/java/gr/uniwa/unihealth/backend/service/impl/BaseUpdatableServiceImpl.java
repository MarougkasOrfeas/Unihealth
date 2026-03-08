package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.BaseUpdatableDTO;
import gr.uniwa.unihealth.backend.mapper.BaseUpdatableEntityMapper;
import gr.uniwa.unihealth.backend.model.BaseUpdatableEntity;
import gr.uniwa.unihealth.backend.service.BaseUpdatableService;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation for {@link BaseUpdatableService}.
 *
 * @author omaro
 */
@Slf4j
public abstract class BaseUpdatableServiceImpl<D extends BaseUpdatableDTO, E extends BaseUpdatableEntity>
    extends BaseServiceImpl<D, E> implements BaseUpdatableService<D> {

  @Override
  public void update(String id, D dto) {
    validateForUpdate(id, dto);

    E entity = findEntityById(id);

    log.info("Updating {} with id {}", entity.getClass().getSimpleName(), id);

    getMapper().mapForUpdate(dto, entity);
    entity.setVersion(entity.getVersion().longValue() + 1);

    getRepository().save(entity);
  }

  protected void validateForUpdate(String id, D dto) {
    validate(id, dto);
  }

  protected abstract BaseUpdatableEntityMapper<D, E> getMapper();
}
