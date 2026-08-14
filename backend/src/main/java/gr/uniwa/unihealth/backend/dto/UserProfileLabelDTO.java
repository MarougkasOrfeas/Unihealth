package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileLabelDTO {

  private String code;

  private Integer priority;

  public UserProfileLabelDTO(String code, Integer priority) {
    this.code = code;
    this.priority = priority;
  }
}
