package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;

public interface HealthProfileService extends BaseService<HealthProfileDTO> {

  String completeProfile(String username, HealthProfileDTO dto);

  HealthProfileDTO findByCurrentUser(String username);

  void updateCurrentUserProfile(String username, HealthProfileDTO dto);
}
