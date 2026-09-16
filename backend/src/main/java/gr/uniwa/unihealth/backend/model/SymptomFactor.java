package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * One tickable detail of a symptom, e.g. "feels better when resting the muscle" for chest pain.
 *
 * <p>Factors are derived at ingest by splitting the left-hand cell of the NHS causes table, so a
 * row such as <em>"Starts after eating, bringing up food or bitter tasting fluids, feeling full
 * and bloated"</em> becomes three of them.
 *
 * @author omaro
 */
@Getter
@Setter
@Entity
@Table(name = "t_symptom_factor")
public class SymptomFactor extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "symptom_item_id")
  private SymptomItem symptomItem;

  /** Slug of the label, stable across re-ingestion so seeding stays idempotent. */
  private String code;

  private String label;

  /**
   * How much this factor discriminates between the causes of its symptom, as
   * {@code 1 / (number of cause rows mentioning it)}. A factor that appears under every cause
   * tells the reader nothing and is weighted accordingly.
   */
  private Double weight;

  @Column(name = "display_order")
  private Integer displayOrder;
}
