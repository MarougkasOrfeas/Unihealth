package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UnihealthAssistantContentDTO {

  private List<UnihealthAssistantItemDTO> faq;
  private List<UnihealthAssistantItemDTO> services;
  private List<UnihealthAssistantItemDTO> emergency;
  private List<UnihealthAssistantItemDTO> clinics;
}
