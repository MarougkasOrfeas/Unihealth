package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.dto.GroupDTO;

/**
 * Service interface for the groups.
 *
 * @author European Dynamics SA
 */
public interface GroupService extends BaseUpdatableService<GroupDTO> {

  /**
   * Updates the status of the given group.
   *
   * @param id     The id of the group.
   * @param active The new status to set.
   * @return The updated status of the group.
   */
  boolean setGroupStatus(String id, boolean active);
}
