package gr.uniwa.unihealth.backend.dto;

import gr.uniwa.unihealth.backend.model.enums.AssistantSection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnihealthAssistantItemDTO extends BaseUpdatableDTO {
  private String title;
  private String brief;
  private String content;
  private AssistantSection section;
  private Integer displayOrder;
  private Boolean active;
  private String actionLabel;
  private String actionValue;
  private String icon;
}
