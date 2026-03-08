package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.BaseUpdatableDTO;
import gr.uniwa.unihealth.backend.model.BaseUpdatableEntity;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.MappingTarget;

/**
 * The base mapper for all base updatable entity mappers of the application.
 *
 * @author omaro
 */
public abstract class BaseUpdatableEntityMapper<D extends BaseUpdatableDTO, E extends BaseUpdatableEntity>
    extends BaseEntityMapper<D, E> {

  @Override
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  public abstract E mapForCreate(D dto);

  /**
   * Maps a DTO to an existing provided entity for update.
   *
   * @param dto    The input DTO.
   * @param entity The Entity to map to.
   */
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  public abstract void mapForUpdate(D dto, @MappingTarget E entity);
}
