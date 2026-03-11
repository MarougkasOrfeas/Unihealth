package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "t_group")
public class UniGroup extends BaseUpdatableEntity {

  @Column(updatable = false)
  private String name;

  private String description;

  private boolean active;

  @OneToMany(mappedBy = "group", cascade = CascadeType.REMOVE)
  private List<Department> departments = new ArrayList<>();
}
