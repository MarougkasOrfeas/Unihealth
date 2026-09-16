package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SymptomItemDTO extends BaseUpdatableDTO {

  private String title;
  private String slug;
  private String startingLetter;
  private String brief;
  private String synonyms;
  /** Whether this symptom has an advanced search to offer. */
  private boolean hasFactors;
  private Integer displayOrder;
  private boolean active;
}
