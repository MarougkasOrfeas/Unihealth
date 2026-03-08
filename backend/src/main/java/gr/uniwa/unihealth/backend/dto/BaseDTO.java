package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The core abstract DTO class for all DTOs of the application.
 *
 * @author omaro
 */
@Getter
@Setter
public abstract class BaseDTO {

  private String id;
  private String createdBy;
  private LocalDateTime createdOn;
}
