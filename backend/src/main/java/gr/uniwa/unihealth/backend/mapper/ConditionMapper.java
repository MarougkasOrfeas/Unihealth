package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.ConditionDTO;
import gr.uniwa.unihealth.backend.dto.ConditionDetailDTO;
import gr.uniwa.unihealth.backend.model.Condition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public abstract class ConditionMapper extends BaseUpdatableEntityMapper<ConditionDTO, Condition> {

  /** {@code relatedSymptoms} is filled by the service, which is the only thing that can query it. */
  @Mapping(target = "relatedSymptoms", ignore = true)
  public abstract ConditionDetailDTO mapToDetailDTO(Condition entity);
}
