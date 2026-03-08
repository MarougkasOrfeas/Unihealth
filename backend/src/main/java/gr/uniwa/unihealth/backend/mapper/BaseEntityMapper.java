package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.InheritConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The base mapper for all base entity mappers of the application.
 *
 * @author omaro
 */
public abstract class BaseEntityMapper<D extends BaseDTO, E extends BaseEntity> {

  @Autowired
  protected BaseRepository<E> repository;

  /**
   * Maps a DTO to an entity for create.
   *
   * @param dto The input DTO.
   * @return The mapped Entity.
   */
  @InheritConfiguration(name = "mapDtoToEntityConfig")
  public abstract E mapForCreate(D dto);

  /**
   * Maps an Entity to a DTO.
   *
   * @param entity The input Entity.
   * @return The mapped DTO.
   */
  public abstract D mapToDTO(E entity);

  protected E mapFromIdToEntity(String id) {
    return StringUtils.isBlank(id) ? null : repository.getReferenceById(id);
  }

  protected String mapFromEntityToId(E entity) {
    return entity == null ? null : entity.getId();
  }
}
