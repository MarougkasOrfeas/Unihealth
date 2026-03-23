package gr.uniwa.unihealth.backend.model;

import gr.uniwa.unihealth.backend.model.converter.LabelListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "t_user_profile_labels")
public class UserProfileLabels extends BaseUpdatableEntity{

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Convert(converter = LabelListConverter.class)
  private List<String> labels;

}
