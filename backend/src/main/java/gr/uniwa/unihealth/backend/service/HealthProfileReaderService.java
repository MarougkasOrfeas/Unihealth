package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;

public interface HealthProfileReaderService extends BaseReaderService<HealthProfileDTO> {

  HealthProfileDTO findByCurrentUser(String username);
}
