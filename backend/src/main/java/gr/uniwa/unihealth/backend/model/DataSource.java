package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * An external corpus the application ingests content from, together with the licence under which
 * it may be reused.
 *
 * <p>This exists as a table rather than as constants so that attribution is data: the frontend
 * renders whatever is in here, and a new source cannot be added without also stating its licence
 * and where it came from.
 *
 * @author omaro
 */
@Getter
@Setter
@Entity
@Table(name = "t_data_source")
public class DataSource extends BaseUpdatableEntity {

  /** Stable machine key, e.g. {@code NHS}. */
  private String code;

  private String name;

  private String url;

  private String licence;

  @Column(name = "licence_url")
  private String licenceUrl;

  /** The wording the publisher asks to be shown, rendered verbatim in the UI. */
  @Column(name = "attribution_text")
  private String attributionText;

  @Column(name = "logo_url")
  private String logoUrl;

  @Column(name = "retrieved_on")
  private LocalDate retrievedOn;

  @Column(name = "display_order")
  private Integer displayOrder;

  private boolean active;
}
