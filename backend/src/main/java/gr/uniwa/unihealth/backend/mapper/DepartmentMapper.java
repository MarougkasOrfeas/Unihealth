package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.DepartmentDTO;
import gr.uniwa.unihealth.backend.model.Department;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public abstract class DepartmentMapper
    extends BaseUpdatableEntityMapper<DepartmentDTO, Department> {

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
  public abstract DepartmentDTO mapToDTO(Department entity);

}
