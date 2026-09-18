package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * One surface form a student might write, and the label code it resolves to.
 *
 * <p>The rows are a projection of {@code resources/data/labels/medical-terms.json} — that file is
 * the source of truth and this table is rebuilt from it whenever its checksum changes. Editing rows
 * directly is possible but will be undone on the next dictionary change.
 */
@Getter
@Setter
@Entity
@Table(name = "t_label_keyword_mapping")
public class LabelKeywordMapping extends BaseUpdatableEntity {

  /** The surface form, always stored lowercased so the unique constraint means what it says. */
  private String keyword;

  @Column(name = "label_code")
  private String labelCode;

  /** ALLERGY or CHRONIC — which free-text field this term may be matched against. */
  @Column(name = "keyword_type")
  private String keywordType;

  /** BCP-47, only {@code en} or {@code el}. Greeklish keys are derived, never stored. */
  private String lang;

  /**
   * PREFERRED, SYNONYM, INFLECTION, ABBREVIATION, MISSPELLING or WEAK.
   *
   * <p>Load-bearing rather than descriptive: the last three are never fuzzy-matched, and WEAK terms
   * only count when nothing else matched the same field.
   */
  @Column(name = "term_type")
  private String termType;

  /** Groups every surface form of one idea across both languages. */
  @Column(name = "concept_id")
  private String conceptId;

  /** Per-concept significance, used in place of the flat prefix fallback when ranking labels. */
  private Integer priority;

  /** Where the term came from. Never read at runtime; it is the dataset's citation trail. */
  private String source;

  private boolean active;
}
