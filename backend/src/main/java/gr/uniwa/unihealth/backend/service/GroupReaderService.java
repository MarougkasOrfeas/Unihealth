package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.GroupDTO;

import java.util.List;

/**
 * Service interface for reading group data.
 *
 * @author omaro
 */
public interface GroupReaderService extends BaseReaderService<GroupDTO> {

  /**
   * Finds all active groups.
   *
   * @return List of active groups.
   */
  List<GroupDTO> findAllActive();
}
