package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The core abstract DTO class for all DTOs of the application that can be updated.
 *
 * @author omaro
 */
@Getter
@Setter
public abstract class BaseUpdatableDTO extends BaseDTO {

  private String modifiedBy;
  private LocalDateTime modifiedOn;
}
