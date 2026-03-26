package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.dto.HealthProfileViewDTO;

public interface HealthProfileService extends BaseService<HealthProfileDTO> {

  String completeProfile(String username, HealthProfileDTO dto);

  HealthProfileViewDTO findByCurrentUser(String username);

  HealthProfileViewDTO updateCurrentUserProfile(String username, HealthProfileDTO dto);
}
