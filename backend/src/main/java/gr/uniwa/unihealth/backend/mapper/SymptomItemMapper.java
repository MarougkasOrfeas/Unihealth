package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.SymptomItemDTO;
import gr.uniwa.unihealth.backend.dto.SymptomItemDetailDTO;
import gr.uniwa.unihealth.backend.model.SymptomItem;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public abstract class SymptomItemMapper
    extends BaseUpdatableEntityMapper<SymptomItemDTO, SymptomItem> {

  public abstract SymptomItemDetailDTO mapToDetailDTO(SymptomItem entity);
}
