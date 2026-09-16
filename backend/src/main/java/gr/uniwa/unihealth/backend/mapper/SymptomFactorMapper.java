package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.SymptomFactorDTO;
import gr.uniwa.unihealth.backend.model.SymptomFactor;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public abstract class SymptomFactorMapper
    extends BaseUpdatableEntityMapper<SymptomFactorDTO, SymptomFactor> {

}
