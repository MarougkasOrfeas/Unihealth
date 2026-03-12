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
  private Integer displayOrder;
  private boolean active;
}
