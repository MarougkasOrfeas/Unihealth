package gr.uniwa.unihealth.backend.service.personalization;

import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.dto.UserProfileLabelDTO;

import java.util.List;

public interface UserProfileLabelsService {

  /**
   * Upserts the significance-sorted label list for a given user. Creates a new record on first
   * profile completion, updates the existing one on subsequent profile edits.
   */
  void saveForUser(User user, List<String> sortedLabels);

  void saveOptionalForUser(User user, List<String> sortedOptionalLabels);

  List<UserProfileLabelDTO> findForUser(String userId);
}
