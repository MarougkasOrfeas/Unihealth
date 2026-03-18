package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.OptionalHealthProfileDTO;
import gr.uniwa.unihealth.backend.model.OptionalHealthProfile;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public abstract class OptionalHealthProfileMapper extends BaseUpdatableEntityMapper<OptionalHealthProfileDTO, OptionalHealthProfile>{

}
