package gr.uniwa.unihealth.backend.mapper;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.eurodyn.qlack.fuse.lexicon.repository.LanguageRepository;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.model.Department;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.UserRoles;
import gr.uniwa.unihealth.backend.repository.DepartmentRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(config = MappingConfig.class)
public abstract class UserMapper extends BaseUpdatableEntityMapper<UserDTO, User> {

  @Autowired
  private LanguageRepository languageRepository;

  @Autowired
  private DepartmentRepository departmentRepository;

  @Override
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "status",
      expression = "java(gr.uniwa.unihealth.backend.model.enums.UserStatus.UNVERIFIED)")
  // Role comes from the request so an administrator can create another administrator, defaulting
  // to USER when absent. UserController.create requires the ADMIN permission, which is what stops
  // a student from asking for the ADMIN role. Expressed here rather than in afterMapToEntity
  // because that hook also runs for mapForUpdate, which must keep ignoring the role.
  @Mapping(target = "role", expression = "java(toRole(dto.getRole()))")
  @Mapping(target = "lastLogin", ignore = true)
  @Mapping(target = "emailSentNoLoginSince", ignore = true)
  @Mapping(target = "deactivateOn", ignore = true)
  @Mapping(target = "deactivatedDueToInactivity", ignore = true)
  @Mapping(target = "language", ignore = true)
  @Mapping(target = "deactivationReason", ignore = true)
  @Mapping(target = "reactivationReason", ignore = true)
  @Mapping(target = "department", ignore = true)
  @Mapping(target = "healthProfileCompleted", ignore = true)
  @Mapping(target = "healthProfileCompletedOn", ignore = true)
  public abstract User mapForCreate(UserDTO dto);

  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "lastLogin", ignore = true)
  @Mapping(target = "emailSentNoLoginSince", ignore = true)
  @Mapping(target = "deactivateOn", ignore = true)
  @Mapping(target = "deactivatedDueToInactivity", ignore = true)
  @Mapping(target = "language", ignore = true)
  @Mapping(target = "deactivationReason", ignore = true)
  @Mapping(target = "reactivationReason", ignore = true)
  @Mapping(target = "department", ignore = true)
  @Mapping(target = "healthProfileCompleted", ignore = true)
  @Mapping(target = "healthProfileCompletedOn", ignore = true)
  public abstract void mapForUpdate(UserDTO dto, @MappingTarget User entity);


  @Override
  @Mapping(target = "language", ignore = true)
  @Mapping(target = "group", ignore = true)
  @Mapping(target = "department", ignore = true)
  public abstract UserDTO mapToDTO(User entity);

  @AfterMapping
  protected void afterMapToDTO(User entity, @MappingTarget UserDTO dto) {
    dto.setLanguage(entity.getLanguage().getId());
    if (entity.getDepartment() != null) {
      dto.setDepartment(entity.getDepartment().getName());

      if (entity.getDepartment().getGroup() != null) {
        dto.setGroup(entity.getDepartment().getGroup().getName());
      }
    }
  }

  /**
   * Parses the requested role, falling back to {@code USER}. An unknown value is rejected rather
   * than silently downgraded, so a typo surfaces instead of quietly creating the wrong account.
   */
  protected UserRoles toRole(String role) {
    if (role == null || role.isBlank()) {
      return UserRoles.USER;
    }
    try {
      return UserRoles.valueOf(role.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown user role: " + role);
    }
  }

  @AfterMapping
  protected void afterMapToEntity(UserDTO dto, @MappingTarget User entity) {
    entity.setLanguage(languageRepository.getReferenceById(dto.getLanguage()));

    // The department is optional on the entity: the deployment administrator created at startup
    // is staff, not a student, and belongs to no school. The create/edit form still requires one,
    // so every user registered through the UI has a department.
    if (dto.getDepartment() == null || dto.getDepartment().isBlank()) {
      entity.setDepartment(null);
      return;
    }

    Department department = departmentRepository.findByNameIgnoreCase(dto.getDepartment())
        .orElseThrow(() -> new QDoesNotExistException("Department does not exist"));

    if (dto.getGroup() != null && !dto.getGroup().isBlank()) {
      if (department.getGroup() == null || !dto.getGroup()
          .equals(department.getGroup().getName())) {
        throw new IllegalArgumentException("Department does not belong to the selected group");
      }
    }

    entity.setDepartment(department);
  }

}
