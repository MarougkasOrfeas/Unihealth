package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConditionDTO extends BaseUpdatableDTO {

  private String name;
  private String slug;
  private String startingLetter;
  private String sourceUrl;
  private Integer displayOrder;
  private boolean active;
}
