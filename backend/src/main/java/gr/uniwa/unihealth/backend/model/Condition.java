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
 * A possible cause of a symptom, listed in the conditions A-Z.
 *
 * <p>Only the name and a deep link are held. The NHS robots.txt disallows {@code /Conditions/}, so
 * condition pages are never fetched; everything here is harvested from the causes tables on the
 * symptom pages, which are allowed.
 *
 * @author omaro
 */
@Getter
@Setter
@Entity
@Table(name = "t_condition")
public class Condition extends BaseUpdatableEntity {

  private String name;

  private String slug;

  @Column(name = "starting_letter")
  private String startingLetter;

  /** Where a reader can go to read about this condition at the publisher. */
  @Column(name = "source_url")
  private String sourceUrl;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_id")
  private DataSource source;

  @Column(name = "display_order")
  private Integer displayOrder;

  private boolean active;
}
