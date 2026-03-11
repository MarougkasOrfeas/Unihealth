package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "t_department")
public class Department extends BaseUpdatableEntity {

  @Column(updatable = false)
  private String name;

  private String description;

  private boolean active;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id")
  private UniGroup group;

  @OneToMany(mappedBy = "department")
  private List<User> users = new ArrayList<>();
}
