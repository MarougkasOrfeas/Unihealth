package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_label_field_mapping")
public class LabelFieldMapping extends BaseUpdatableEntity {

  @Column(name = "label_code")
  private String labelCode;

  @Column(name = "field_name")
  private String fieldName;

  private String operator;

  @Column(name = "match_value")
  private String matchValue;

  @Column(name = "match_value2")
  private String matchValue2;

  @Column(name = "calculated_field")
  private boolean calculatedField;

  @Column(name = "mixed_rule")
  private boolean mixedRule;

  @Column(name = "label_group")
  private String labelGroup;

  private String description;

  private boolean active;

  private Integer priority;
}
