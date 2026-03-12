package gr.uniwa.unihealth.backend.model;

import gr.uniwa.unihealth.backend.model.enums.AssistantSection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_unihealth_assistant_item")
public class UnihealthAssistantItem extends BaseUpdatableEntity {

  private String title;

  private String brief;

  private String content;

  @Enumerated(EnumType.STRING)
  private AssistantSection section;

  @Column(name = "display_order")
  private Integer displayOrder;

  private Boolean active;

  @Column(name = "action_label")
  private String actionLabel;

  @Column(name = "action_value")
  private String actionValue;

  private String icon;
}
