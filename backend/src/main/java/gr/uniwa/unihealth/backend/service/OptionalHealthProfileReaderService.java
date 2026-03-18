package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.OptionalHealthProfileDTO;

public interface OptionalHealthProfileReaderService extends BaseReaderService<OptionalHealthProfileDTO> {
  OptionalHealthProfileDTO findByCurrentUser(String username);
}
