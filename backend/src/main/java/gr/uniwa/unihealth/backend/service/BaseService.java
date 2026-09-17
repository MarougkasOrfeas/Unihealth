package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.dto.BaseDTO;

import java.util.Collection;

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
   * Saves a collection of new entities in the database.
   *
   * @param dtos The collection of DTOs with the data to save.
   * @return The ids of the saved entities.
   */
  Collection<String> create(Collection<D> dtos);

  /**
   * Deletes an existing entity by it's id.
   *
   * @param id The id of the entity to delete.
   */
  void delete(String id);

  /**
   * Deletes a collection of existing entities by their ids.
   *
   * @param ids The collection of ids of the entities to delete.
   */
  void delete(Collection<String> ids);

  /**
   * Validates if the entity is available for creation or update.
   *
   * @param id  The id of the entity to validate.
   * @param dto The DTO with the data to validate.
   */
  void validateAvailable(String id, D dto);
}
