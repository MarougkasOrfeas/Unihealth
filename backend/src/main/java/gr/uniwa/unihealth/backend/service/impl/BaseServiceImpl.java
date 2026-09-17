package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Implementation for {@link BaseService}.
 *
 * @author omaro
 */
@Slf4j
@Transactional
public abstract class BaseServiceImpl<D extends BaseDTO, E extends BaseEntity>
    implements BaseService<D> {

  @Autowired
  protected AuthenticationContext authenticationContext;

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
  public Collection<String> create(Collection<D> dtos) {
    return Optional.ofNullable(dtos).orElse(List.of()).stream().map(this::create).toList();
  }

  @Override
  public void delete(String id) {
    E entity = findEntityById(id);
    validateForDelete(id, entity);
    log.info("Deleting {} with id {}", entity.getClass().getSimpleName(), entity.getId());
    getRepository().delete(entity);
  }

  @Override
  public void delete(Collection<String> ids) {
    Optional.ofNullable(ids).orElse(List.of()).stream().filter(StringUtils::isNotBlank).distinct()
        .forEach(this::delete);
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
    validateAvailable(id, dto);
  }
}
