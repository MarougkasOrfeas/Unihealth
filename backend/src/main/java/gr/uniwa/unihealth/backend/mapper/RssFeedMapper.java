package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.RssFeedDTO;
import gr.uniwa.unihealth.backend.model.RssFeed;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public abstract class RssFeedMapper extends BaseUpdatableEntityMapper<RssFeedDTO, RssFeed> {
}
