package gr.uniwa.unihealth.backend.dto;

import gr.uniwa.unihealth.backend.model.enums.Permission;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO representing basic user information and thei permissions.
 *
 * @author omaro
 */
@Getter
@Setter
public class RightsMatrix implements Serializable {

  private String userId;
  private UserStatus userStatus;
  private Set<Permission> globalPermissions = new HashSet<>();
}
