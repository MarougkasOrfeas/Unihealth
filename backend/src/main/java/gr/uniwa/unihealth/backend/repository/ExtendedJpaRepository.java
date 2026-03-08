package gr.uniwa.unihealth.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

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
}
