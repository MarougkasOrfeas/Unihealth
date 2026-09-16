package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One row of the NHS "symptoms and possible causes" table: a set of factors on the left, and the
 * condition or conditions they point to on the right.
 *
 * <p>This is the unit the advanced search scores. A row can name more than one condition
 * ("Heartburn or indigestion"), which is why {@link #conditions} is a collection.
 *
 * @author omaro
 */
@Getter
@Setter
@Entity
@Table(name = "t_symptom_cause")
public class SymptomCause extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "symptom_item_id")
  private SymptomItem symptomItem;

  /** The left-hand cell verbatim, kept so the split into factors stays auditable. */
  @Column(name = "factors_text")
  private String factorsText;

  /**
   * The right-hand cell verbatim. About a third of the NHS rows name the cause only in prose, with
   * no link to a condition page, so this - not {@link #conditions} - is what the UI shows.
   */
  @Column(name = "cause_text")
  private String causeText;

  @Column(name = "display_order")
  private Integer displayOrder;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "t_symptom_cause_condition",
      joinColumns = @JoinColumn(name = "symptom_cause_id"),
      inverseJoinColumns = @JoinColumn(name = "condition_id"))
  @OrderBy("name asc")
  private Set<Condition> conditions = new LinkedHashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "t_symptom_cause_factor",
      joinColumns = @JoinColumn(name = "symptom_cause_id"),
      inverseJoinColumns = @JoinColumn(name = "symptom_factor_id"))
  @OrderBy("displayOrder asc")
  private Set<SymptomFactor> factors = new LinkedHashSet<>();
}
