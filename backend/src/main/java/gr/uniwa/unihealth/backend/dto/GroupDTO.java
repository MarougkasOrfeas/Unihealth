package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GroupDTO extends BaseUpdatableDTO {

  private String name;
  private String description;
  private boolean active;
  private List<String> departments = new ArrayList<>();
}
