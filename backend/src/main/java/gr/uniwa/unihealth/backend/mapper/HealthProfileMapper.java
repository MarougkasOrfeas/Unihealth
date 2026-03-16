package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.model.HealthProfile;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public abstract class HealthProfileMapper
    extends BaseUpdatableEntityMapper<HealthProfileDTO, HealthProfile> {

  @Override
  @Mapping(target = "user", ignore = true)
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  public abstract HealthProfile mapForCreate(HealthProfileDTO dto);

  @Mapping(target = "user", ignore = true)
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  public abstract void mapForUpdate(HealthProfileDTO dto, @MappingTarget HealthProfile entity);

}
