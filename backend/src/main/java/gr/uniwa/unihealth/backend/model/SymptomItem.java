package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_symptom_item")
public class SymptomItem extends BaseUpdatableEntity {

  private String title;
  private String slug;

  @Column(name = "starting_letter")
  private String startingLetter;

  private String brief;

  @Column(name = "overview_text")
  private String overviewText;

  @Column(name = "symptoms_text")
  private String symptomsText;

  @Column(name = "do_text")
  private String doText;

  @Column(name = "dont_text")
  private String dontText;

  @Column(name = "see_doctor_if_text")
  private String seeDoctorIfText;

  @Column(name = "treatment_text")
  private String treatmentText;

  @Column(name = "causes_text")
  private String causesText;

  @Column(name = "display_order")
  private Integer displayOrder;

  private boolean active;
}
