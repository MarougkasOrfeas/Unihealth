package gr.uniwa.unihealth.backend.config.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Provides information about the logged in user.
 *
 * @author omaro
 */
@Component
public class AuthenticationContext {

  public static final String TENANT_CLAIM_NAME = "tenant";
  private static final String SYSTEM_USER = "system";

  /**
   * Returns the username of the currently logged in user. If no user is logged in, returns
   * "system".
   *
   * @return The current logged in username.
   */
  public String getCurrentUsername() {
    return getClaimAsStringOr("preferred_username", SYSTEM_USER);
  }

  /**
   * Returns the tenant ID of the currently logged in user. If no user is logged in or the claim is
   * not present, returns null.
   *
   * @return The current logged in tenant id.
   */
  public String getCurrentUserTenantId() {
    return getClaimAsStringOr(TENANT_CLAIM_NAME, null);
  }

  private String getClaimAsStringOr(String claimName, String defaultValue) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return defaultValue;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof Jwt jwt) {
      return jwt.getClaimAsString(claimName);
    }

    return defaultValue;
  }
}
