package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.DepartmentDTO;

public interface DepartmentService extends BaseUpdatableService<DepartmentDTO> {

  /**
   * Updates the status of the given department.
   *
   * @param id     The id of the department.
   * @param active The new status to set.
   * @return The updated status of the department.
   */
  boolean setDepartmentStatus(String id, boolean active);
}
