package gr.uniwa.unihealth.backend.config.filter;

import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.exception.UNIHEALTHException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter to determine the tenant from the request.
 *
 * @author omaro
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

  private static final List<String> EXCLUDED_PATHS =
      List.of("/swagger-ui", "/v3/api-docs", "/api-docs");

  private final TenantContext tenantContext;
  private final AuthenticationContext authenticationContext;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String tenant = getRequestTenant();

    if (tenant != null) {
      tenantContext.runAs(tenant, () -> {
        try {
          filterChain.doFilter(request, response);
        } catch (IOException | ServletException e) {
          throw new UNIHEALTHException(null, "Could not process request.", e);
        }
      }, false);
    } else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Could not determine tenant from request.");
    }
  }

  private String getRequestTenant() {
    return tenantContext.getSingleTenantOr(() -> authenticationContext.getCurrentUserTenantId());
  }
}
