package gr.uniwa.unihealth.backend.service;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.dto.FacetDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The core reader service interface for all entities of the application.
 *
 * @author omaro
 */
public interface BaseReaderService<D extends BaseDTO> {

  /**
   * Finds a single DTO by id, wrapped in an {@link Optional}.
   *
   * @param id The id of the DTO.
   * @return An {@link Optional} containing the DTO if found, or empty if not found.
   */
  Optional<D> findOptionalById(String id);

  /**
   * Finds a single DTO by id.
   *
   * @param id The id of the DTO.
   * @return The DTO.
   */
  D findById(String id);

  /**
   * Finds all pageable DTOs based on the given parameters.
   *
   * @param parameters The parameters for filtering and pagination.
   * @return A page of DTOs matching the given parameters.
   */
  Page<D> findAll(Map<String, Object> parameters);

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

  /**
   * Returns DTOs for the requested ids in one repository call, allowing API consumers to resolve
   * related records without issuing one request per id.
   *
   * @param ids The ids to resolve.
   * @return A map of DTOs keyed by id.
   */
  Map<String, D> findByIds(Collection<String> ids);

  /**
   * Loads the facet options for the given facet parameters.
   *
   * @param facetParams The facet parameters for which the facet options should be loaded.
   * @return The facet options for the given facet parameters.
   */
  List<String> loadFacetOptions(FacetDTO facetParams);
}
