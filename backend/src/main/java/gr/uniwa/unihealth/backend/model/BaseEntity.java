package gr.uniwa.unihealth.backend.model;

import com.querydsl.core.annotations.QuerySupertype;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * The core abstract Entity class for all entities of the application.
 *
 * @author omaro
 */
@Getter
@Setter
@QuerySupertype
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

  @Id
  @UuidGenerator
  @GeneratedValue
  @Column(updatable = false)
  private String id;

  @CreatedBy
  @Column(name = "created_by", updatable = false)
  private String createdBy;

  @CreatedDate
  @Column(name = "created_on", updatable = false)
  private LocalDateTime createdOn;
}
