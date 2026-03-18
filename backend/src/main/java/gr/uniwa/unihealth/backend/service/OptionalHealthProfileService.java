package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.OptionalHealthProfileDTO;

public interface OptionalHealthProfileService extends BaseService<OptionalHealthProfileDTO>{

  void updateCurrentUserOptionalProfile(String username, OptionalHealthProfileDTO dto);
}
