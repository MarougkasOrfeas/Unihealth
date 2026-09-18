package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileLabelDTO {

  private String code;

  private Integer priority;

  /**
   * The family this label belongs to, e.g. "bmi", "optional_sleep", "optional_nutrition". Comes
   * from {@code LabelFieldMapping.labelGroup} so the frontend can group a user's signals without
   * having to infer families from the shape of the code.
   *
   * Null when the label has no active mapping, which is the same case that falls back to a
   * calculated priority.
   */
  private String labelGroup;

  public UserProfileLabelDTO(String code, Integer priority, String labelGroup) {
    this.code = code;
    this.priority = priority;
    this.labelGroup = labelGroup;
  }
}
