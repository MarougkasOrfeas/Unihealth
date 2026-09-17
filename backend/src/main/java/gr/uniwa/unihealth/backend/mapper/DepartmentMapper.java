package gr.uniwa.unihealth.backend.mapper;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.DepartmentDTO;
import gr.uniwa.unihealth.backend.model.Department;
import gr.uniwa.unihealth.backend.model.UniGroup;
import gr.uniwa.unihealth.backend.repository.UniGroupRepository;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(config = MappingConfig.class)
public abstract class DepartmentMapper
    extends BaseUpdatableEntityMapper<DepartmentDTO, Department> {

  @Autowired
  private UniGroupRepository uniGroupRepository;

  @Override
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "active", expression = "java(true)")
  @Mapping(target = "users", ignore = true)
  @Mapping(target = "group", ignore = true)
  public abstract Department mapForCreate(DepartmentDTO dto);

  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "users", ignore = true)
  @Mapping(target = "group", ignore = true)
  public abstract void mapForUpdate(DepartmentDTO dto, @MappingTarget Department entity);

  @Override
  @Mapping(target = "users", ignore = true)
  @Mapping(target = "group", ignore = true)
  public abstract DepartmentDTO mapToDTO(Department entity);

  @AfterMapping
  protected void afterMapToDTO(Department entity, @MappingTarget DepartmentDTO dto) {
    if (entity.getGroup() != null) {
      dto.setGroup(entity.getGroup().getName());
    }
  }

  /**
   * Resolves the school by name. Applied to both create and update, so a department is never left
   * without a school — one with a null group is excluded from
   * {@code findAllActiveByGroupName} and would therefore never appear in the user-creation cascade.
   */
  @AfterMapping
  protected void afterMapToEntity(DepartmentDTO dto, @MappingTarget Department entity) {
    if (dto.getGroup() == null || dto.getGroup().isBlank()) {
      throw new QDoesNotExistException("School does not exist");
    }

    UniGroup group = uniGroupRepository.findByNameIgnoreCase(dto.getGroup())
        .orElseThrow(() -> new QDoesNotExistException("School does not exist"));

    entity.setGroup(group);
  }

}
