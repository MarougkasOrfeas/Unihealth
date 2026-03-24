package gr.uniwa.unihealth.backend.service.permission.resolver;


import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.RightsMatrix;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.Permission;
import gr.uniwa.unihealth.backend.model.enums.UserRoles;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RightsMatrixResolver {

  private final UserRepository userRepository;

  @Cacheable(value = "rightsMatrix", key = "#username")
  public RightsMatrix getRightsMatrixByUsername(String username) {
    User user = userRepository.findByUsername(username).orElseThrow(
        () -> new QDoesNotExistException("User with username " + username + " does not exist"));

    RightsMatrix rightsMatrix = new RightsMatrix();
    rightsMatrix.setUserId(user.getId());
    rightsMatrix.setUserStatus(user.getStatus());

    if (UserStatus.ACTIVE.equals(user.getStatus()) && user.getRole() == UserRoles.ADMIN) {
      rightsMatrix.getGlobalPermissions().add(Permission.ADMIN);
    }

    return rightsMatrix;
  }

  @CacheEvict(value = "rightsMatrix", key = "#username")
  public void evictByUsername(String username) {
    // empty method – Spring handles eviction
  }
}
