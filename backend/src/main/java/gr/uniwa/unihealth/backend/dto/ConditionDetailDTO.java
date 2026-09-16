package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A condition page.
 *
 * <p>There is no description: the NHS robots.txt disallows {@code /Conditions/}, so those pages are
 * never ingested. What a reader gets instead is the link out to the publisher and the list of
 * symptoms whose causes table points here, which is the part this application actually knows.
 */
@Getter
@Setter
public class ConditionDetailDTO extends BaseUpdatableDTO {

  private String name;
  private String slug;
  private String startingLetter;
  private String sourceUrl;
  private DataSourceDTO source;
  private List<SymptomItemDTO> relatedSymptoms;
  private boolean active;
}
