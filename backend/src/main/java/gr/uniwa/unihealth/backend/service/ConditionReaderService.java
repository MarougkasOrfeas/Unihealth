package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.ConditionDTO;
import gr.uniwa.unihealth.backend.dto.ConditionDetailDTO;

import java.util.List;

public interface ConditionReaderService extends BaseReaderService<ConditionDTO> {

  List<ConditionDTO> findContent(String search);

  ConditionDetailDTO findBySlug(String slug);
}
