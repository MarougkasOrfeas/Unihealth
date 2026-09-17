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

  /**
   * Name of the school this department belongs to. Resolved to/from the {@code group} association
   * by {@link gr.uniwa.unihealth.backend.mapper.DepartmentMapper}, so a department created from the
   * UI is attached to a school and therefore shows up in the user-creation cascade.
   */
  private String group;

  private List<String> users = new ArrayList<>();

}
