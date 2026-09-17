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

  @Column(name = "health_profile_completed")
  private boolean healthProfileCompleted;

  @Column(name = "health_profile_completed_on")
  private LocalDateTime healthProfileCompletedOn;

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

  /**
   * Whether the user still wants the health-news digest. Stored positive: the profile screen shows
   * it as "unsubscribe", and persisting the negative invites double-negative bugs.
   */
  @Column(name = "newsletter_subscribed", nullable = false)
  private boolean newsletterSubscribed = true;

  /**
   * Whether the user accepts occasional non-critical notifications, e.g. the reminder to finish
   * the optional health profile. Account-critical mail ignores this — see EmailPreferenceService.
   */
  @Column(name = "notifications_enabled", nullable = false)
  private boolean notificationsEnabled = true;

  /** How many optional-profile reminders have gone out, so the reminder never becomes spam. */
  @Column(name = "optional_form_reminders_sent", nullable = false)
  private int optionalFormRemindersSent;

  @Column(name = "optional_form_reminder_last_sent_on")
  private LocalDateTime optionalFormReminderLastSentOn;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  private Department department;
}
