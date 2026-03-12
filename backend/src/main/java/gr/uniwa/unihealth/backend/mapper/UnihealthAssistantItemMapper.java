package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.UnihealthAssistantItemDTO;
import gr.uniwa.unihealth.backend.model.UnihealthAssistantItem;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public abstract class UnihealthAssistantItemMapper
    extends BaseUpdatableEntityMapper<UnihealthAssistantItemDTO, UnihealthAssistantItem> {


}
