package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.dto.HealthProfileViewDTO;

public interface HealthProfileReaderService extends BaseReaderService<HealthProfileDTO> {

  HealthProfileViewDTO findByCurrentUser(String username);
}
