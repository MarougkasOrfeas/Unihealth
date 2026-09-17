package gr.uniwa.unihealth.backend.repository;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.dto.FacetDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * This interface is an extension of {@link JpaRepository} and adds more functions to it.
 *
 * @param <E> The type of the entity managed by this repository.
 * @param <I> The id of the entity managed by this repository.
 * @author omaro
 */
@NoRepositoryBean
public interface ExtendedJpaRepository<E, I> extends JpaRepository<E, I> {

  /**
   * Flushes all pending changes to the database and then refreshes the state of the given entity
   * from the database.
   *
   * @param entity The entity to refresh.
   */
  void flushAndRefresh(E entity);

  /**
   * Loads an entity by its id with a write lock, so that a value derived from its current state
   * (e.g. a sequence number) can be read and incremented without two concurrent callers deriving
   * the same value. The lock is held until the transaction commits and only covers the single row
   * of the given entity.
   *
   * <p>The caller must run in a writable transaction. In a {@code readOnly = true} transaction the
   * pending changes are never flushed, so the update would be silently lost.
   *
   * @param id The id of the entity to load and lock.
   * @return The locked entity, if it exists.
   */
  Optional<E> findByIdForUpdate(I id);

  /**
   * Finds all pageable entities.
   *
   * @param predicate The {@link Predicate} that the results must fulfill.
   * @param pageable  Pagination requirements.
   * @return The result entities.
   */
  Page<E> findPage(Predicate predicate, Pageable pageable);

  /**
   * Loads the facet options for the given facet parameters.
   *
   * @param facetParams The facet parameters for which the facet options should be loaded.
   * @param predicate   The {@link Predicate} that the facet options must fulfill.
   * @return The facet options for the given facet parameters.
   */
  List<String> loadFacetOptions(FacetDTO facetParams, Predicate predicate);
}
