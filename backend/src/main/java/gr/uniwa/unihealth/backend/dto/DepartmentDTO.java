package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DepartmentDTO extends BaseUpdatableDTO {

  private String name;
  private String description;
  private boolean active;
  private List<String> users = new ArrayList<>();

}
