package gr.uniwa.unihealth.backend.model;

import com.querydsl.core.annotations.QuerySupertype;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * The core abstract Entity class for entities of the application that can be updated.
 *
 * @author omaro
 */
@Getter
@Setter
@QuerySupertype
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@OptimisticLocking(type = OptimisticLockType.VERSION)
public abstract class BaseUpdatableEntity extends BaseEntity {

  @LastModifiedBy
  @Column(name = "modified_by")
  private String modifiedBy;

  @LastModifiedDate
  @Column(name = "modified_on")
  private LocalDateTime modifiedOn;

  @Version
  private Long version;
}
