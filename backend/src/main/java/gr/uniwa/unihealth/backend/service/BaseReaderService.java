package gr.uniwa.unihealth.backend.service;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.dto.BaseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * The core reader service interface for all entities of the application.
 *
 * @author omaro
 */
public interface BaseReaderService<D extends BaseDTO> {

  /**
   * Finds a single DTO by id.
   *
   * @param id The id of the DTO.
   * @return The DTO.
   */
  D findById(String id);

  /**
   * Finds all pageable DTOs.
   *
   * @param predicate The {@link Predicate} that the results must fulfill.
   * @param pageable  Pagination requirements.
   * @return The result DTOs.
   */
  Page<D> findAll(Predicate predicate, Pageable pageable);

  /**
   * Returns all DTOs as a flat list (no pagination).
   *
   * @return A list of DTOs.
   */
  List<D> findAll();
}
