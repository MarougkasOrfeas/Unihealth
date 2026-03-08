package gr.uniwa.unihealth.backend.model;

import com.eurodyn.qlack.fuse.lexicon.model.Language;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

  @Enumerated(EnumType.STRING)
  private UserStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  private Language language;

  @Column(name = "email_sent_no_login_since")
  private boolean emailSentNoLoginSince;
}
