package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DataSourceDTO extends BaseUpdatableDTO {

  private String code;
  private String name;
  private String url;
  private String licence;
  private String licenceUrl;
  private String attributionText;
  private String logoUrl;
  private LocalDate retrievedOn;
  private Integer displayOrder;
  private boolean active;
}
