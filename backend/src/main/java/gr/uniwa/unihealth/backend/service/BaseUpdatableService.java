package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.dto.BaseUpdatableDTO;

/**
 * The core abstract service interface for all entities of the application that can be updated.
 *
 * @author omaro
 */
public interface BaseUpdatableService<D extends BaseUpdatableDTO> extends BaseService<D> {

  /**
   * Updates an existing entity in the database.
   *
   * @param id  The id of the entity to update.
   * @param dto The DTO with the data to save.
   */
  void update(String id, D dto);
}
