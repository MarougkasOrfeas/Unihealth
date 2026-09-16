package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.DataSourceDTO;
import gr.uniwa.unihealth.backend.model.DataSource;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public abstract class DataSourceMapper
    extends BaseUpdatableEntityMapper<DataSourceDTO, DataSource> {

}
