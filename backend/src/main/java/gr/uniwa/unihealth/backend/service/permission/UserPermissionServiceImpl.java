package gr.uniwa.unihealth.backend.service.permission;


import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.RightsMatrix;
import gr.uniwa.unihealth.backend.service.permission.resolver.RightsMatrixResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {

  private final AuthenticationContext authenticationContext;
  private final RightsMatrixResolver rightsMatrixResolver;

  @Override
  public RightsMatrix getLoggedinUserRightsMatrix() {
    String username = authenticationContext.getCurrentUsername();
    return rightsMatrixResolver.getRightsMatrixByUsername(username);
  }
}
