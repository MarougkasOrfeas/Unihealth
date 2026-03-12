package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.UnihealthAssistantContentDTO;
import gr.uniwa.unihealth.backend.dto.UnihealthAssistantItemDTO;

public interface UnihealthAssistantReaderService
    extends BaseReaderService<UnihealthAssistantItemDTO> {
  UnihealthAssistantContentDTO findContent();

  UnihealthAssistantContentDTO searchContent(String search);
}
