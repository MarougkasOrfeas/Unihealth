package gr.uniwa.unihealth.backend.dto;

import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO extends BaseUpdatableDTO {

  private String username;

  private String email;

  private String lastname;

  private String firstname;

  private UserStatus status;

  private String language;
  
  private boolean emailSentNoLoginSince;
}
