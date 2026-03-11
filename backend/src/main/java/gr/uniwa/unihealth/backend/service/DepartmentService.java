package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.DepartmentDTO;

public interface DepartmentService extends BaseUpdatableService<DepartmentDTO> {

  /**
   * Updates the status of the given group.
   *
   * @param id     The id of the group.
   * @param active The new status to set.
   * @return The updated status of the group.
   */
  boolean setGroupStatus(String id, boolean active);
}
