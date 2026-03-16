package gr.uniwa.unihealth.backend.dto;

import gr.uniwa.unihealth.backend.model.enums.DeactivationMode;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserDTO extends BaseUpdatableDTO {

  private String username;

  private String email;

  private String lastname;

  private String firstname;

  private String phoneNumber;

  private String department;

  private String group;

  private String role;

  private UserStatus status;

  private LocalDateTime lastLogin;

  private LocalDate deactivateAfter;

  private DeactivationMode deactivationMode;

  private String language;

  private boolean emailSentNoLoginSince;

  private LocalDateTime deactivateOn;

  private boolean deactivatedDueToInactivity;

  private String scheduledDeactivationReason;

  private String deactivationReason;

  private String reactivationReason;

  private boolean healthProfileCompleted;
  
  private LocalDateTime healthProfileCompletedOn;
}
