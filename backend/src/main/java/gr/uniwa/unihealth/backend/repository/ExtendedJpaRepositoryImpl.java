package gr.uniwa.unihealth.backend.repository;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * Implementation for {@link ExtendedJpaRepository}.
 *
 * @author omaro
 */
public class ExtendedJpaRepositoryImpl<T, I> extends SimpleJpaRepository<T, I>
    implements ExtendedJpaRepository<T, I> {

  private final EntityManager entityManager;

  /**
   * Creates a new {@link ExtendedJpaRepositoryImpl} to manage objects of the given
   * {@link JpaEntityInformation}.
   *
   * @param jpaEntityInformation must not be {@literal null}.
   * @param entityManager        must not be {@literal null}.
   */
  public ExtendedJpaRepositoryImpl(JpaEntityInformation<T, I> jpaEntityInformation,
      EntityManager entityManager) {
    super(jpaEntityInformation, entityManager);
    this.entityManager = entityManager;
  }

  @Override
  public void flushAndRefresh(T entity) {
    entityManager.flush();
    entityManager.refresh(entity);
  }
}
