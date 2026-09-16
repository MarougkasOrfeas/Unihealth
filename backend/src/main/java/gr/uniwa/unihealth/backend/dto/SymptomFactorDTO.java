package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * One tickable detail of a symptom, as offered to the reader in the advanced search.
 */
@Getter
@Setter
public class SymptomFactorDTO extends BaseUpdatableDTO {

  private String code;
  private String label;
  private Integer displayOrder;
}
