package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.model.BaseUpdatableEntity;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;

/**
 * Mapping Configuration for all mappers.
 *
 * @author omaro
 */
@MapperConfig(componentModel = "spring")
public interface MappingConfig<D extends BaseDTO, E extends BaseEntity, U extends BaseUpdatableEntity> {

  /**
   * Configuration for mapping a DTO to an entity.
   *
   * @param dto The source DTO.
   * @return The target Entity.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdOn", ignore = true)
  E mapDtoToEntityConfig(D dto);

  /**
   * Configuration for mapping a DTO to an updatable entity.
   *
   * @param dto The source DTO.
   * @return The target Entity.
   */
  @InheritConfiguration(name = "mapDtoToEntityConfig")
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "modifiedOn", ignore = true)
  @Mapping(target = "version", ignore = true)
  U mapDtoToUpdatableEntityConfig(D dto);
}
