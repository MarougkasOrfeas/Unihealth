package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Records which version of a file-driven reference dataset is currently loaded into this tenant.
 *
 * <p>This exists to retire {@code if (repository.count() > 0) return;}. That guard meant a seeded
 * table could never be corrected again: once one row existed, editing the seeder was a silent no-op
 * on every environment that had already started once. Storing a checksum of the source file instead
 * makes "edit the file and restart" the whole workflow, and makes it obvious in the log when a
 * dataset has changed.
 *
 * <p>Deliberately not a {@link BaseUpdatableEntity}. It is a ledger keyed by the resource path, not
 * a domain object, so the six audit columns and optimistic-locking version would be noise.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_reference_data_version")
public class ReferenceDataVersion {

  @Id
  @Column(name = "resource_name")
  private String resourceName;

  private String checksum;

  @Column(name = "loaded_on")
  private LocalDateTime loadedOn;

  @Column(name = "row_count")
  private int rowCount;

  public ReferenceDataVersion(String resourceName, String checksum, int rowCount) {
    this.resourceName = resourceName;
    this.checksum = checksum;
    this.rowCount = rowCount;
    this.loadedOn = LocalDateTime.now();
  }
}
