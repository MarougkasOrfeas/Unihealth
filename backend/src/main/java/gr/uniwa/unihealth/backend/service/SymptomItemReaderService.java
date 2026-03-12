package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.SymptomItemDTO;
import gr.uniwa.unihealth.backend.dto.SymptomItemDetailDTO;

import java.util.List;

public interface SymptomItemReaderService extends BaseReaderService<SymptomItemDTO> {

  List<SymptomItemDTO> findContent(String search);

  SymptomItemDetailDTO findBySlug(String slug);
}
