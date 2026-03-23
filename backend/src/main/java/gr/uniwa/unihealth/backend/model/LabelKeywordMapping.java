package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_label_keyword_mapping")
public class LabelKeywordMapping extends BaseUpdatableEntity {

  private String keyword;

  @Column(name = "label_code")
  private String labelCode;

  @Column(name = "keyword_type")
  private String keywordType;

  private boolean active;
}
