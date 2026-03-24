package gr.uniwa.unihealth.backend.service.permission;

import gr.uniwa.unihealth.backend.dto.RightsMatrix;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.model.enums.Permission;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;

public interface UserPermissionService {

  /**
   * Checks if the currently logged in user has at least one of the specified permissions.
   *
   * @param permissions the permissions to check
   * @throws AccessDeniedException if the user does not have any of the specified permissions
   */
  default void userHasGlobalPermissionOrThrow(Permission... permissions) {
    if (!userHasGlobalPermission(permissions)) {
      throw createUnauthorizedException();
    }
  }

  /**
   * Checks if the currently logged in user has at least one of the specified permissions.
   *
   * @param permissions the permissions to check
   * @return true if the user has at least one of the specified permissions, false otherwise
   */
  default boolean userHasGlobalPermission(Permission... permissions) {
    Set<Permission> permissionsToCheck = permissions != null ? Set.of(permissions) : Set.of();
    return getLoggedinUserRightsMatrix().getGlobalPermissions().stream()
        .anyMatch(permissionsToCheck::contains);
  }

  /**
   * Returns the rights matrix for the currently logged in user.
   *
   * @return the rights matrix for the currently logged in user.
   */
  RightsMatrix getLoggedinUserRightsMatrix();

  /**
   * Creates the exception to be thrown for unauthorized access.
   *
   * @return A {@link RuntimeException}.
   */
  default RuntimeException createUnauthorizedException() {
    return ExceptionUtils.createException(AccessDeniedException.class, null,
        "401 - Unauthorized Access");
  }
}
