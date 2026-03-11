package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.GroupDTO;
import gr.uniwa.unihealth.backend.model.UniGroup;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public abstract class UniGroupMapper extends BaseUpdatableEntityMapper<GroupDTO, UniGroup> {

  @Override
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "active", expression = "java(true)")
  @Mapping(target = "departments", ignore = true)
  public abstract UniGroup mapForCreate(GroupDTO dto);

  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "departments", ignore = true)
  public abstract void mapForUpdate(GroupDTO dto, @MappingTarget UniGroup entity);

  @Override
  @Mapping(target = "departments", ignore = true)
  public abstract GroupDTO mapToDTO(UniGroup entity);
}
