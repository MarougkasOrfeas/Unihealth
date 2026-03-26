package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class HealthProfileViewDTO extends HealthProfileDTO {
  private Integer age;
  private BigDecimal bmi;
}
