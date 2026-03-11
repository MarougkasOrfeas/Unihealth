package gr.uniwa.unihealth.backend.model;

import com.eurodyn.qlack.fuse.lexicon.model.Language;
import gr.uniwa.unihealth.backend.model.enums.DeactivationMode;
import gr.uniwa.unihealth.backend.model.enums.UserRoles;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_user")
public class User extends BaseUpdatableEntity {

  @Column(updatable = false)
  private String username;

  private String email;

  private String lastname;

  private String firstname;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "email_sent_no_login_since")
  private boolean emailSentNoLoginSince;

  @ManyToOne(fetch = FetchType.LAZY)
  private Language language;

  @Enumerated(EnumType.STRING)
  private UserStatus status;

  @Enumerated(EnumType.STRING)
  private UserRoles role;

  @Column(name = "last_login")
  private LocalDateTime lastLogin;

  @Column(name = "deactivate_after")
  private LocalDate deactivateAfter;

  @Enumerated(EnumType.STRING)
  @Column(name = "deactivation_mode")
  private DeactivationMode deactivationMode;

  @Column(name = "deactivate_on")
  private LocalDateTime deactivateOn;

  @Column(name = "deactivated_due_to_inactivity")
  private boolean deactivatedDueToInactivity;

  @Column(name = "reactivated_on")
  private LocalDateTime reactivatedOn;

  @Column(name = "deactivation_reason")
  private String deactivationReason;

  @Column(name = "reactivation_reason")
  private String reactivationReason;

  @Column(name = "scheduled_deactivation_reason")
  private String scheduledDeactivationReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  private Department department;
}
