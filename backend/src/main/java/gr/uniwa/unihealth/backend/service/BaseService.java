package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.dto.BaseDTO;

/**
 * The core abstract service interface for all entities of the application.
 *
 * @author omaro
 */
public interface BaseService<D extends BaseDTO> {

  /**
   * Saves a new entity in the database.
   *
   * @param dto The DTO with the data to save.
   * @return The id of the saved entity.
   */
  String create(D dto);

  /**
   * Deletes an existing entity by it's id.
   *
   * @param id The id of the entity to delete.
   */
  void delete(String id);
}
