package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.DepartmentDTO;

import java.util.List;

public interface DepartmentReaderService extends BaseReaderService<DepartmentDTO> {

  /**
   * Finds all active groups.
   *
   * @return List of active groups.
   */
  List<DepartmentDTO> findAllActive();

  List<DepartmentDTO> findAllActiveByGroupName(String groupName);
}
