package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.UserAutomatedDeletionService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.UserService;
import gr.uniwa.unihealth.backend.service.client.KeycloakAdminClient;
import gr.uniwa.unihealth.backend.service.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserAutomatedDeletionServiceImpl implements UserAutomatedDeletionService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final KeycloakAdminClient keycloakAdminClient;
  private final TenantContext tenantContext;
  private final UserReaderService userReaderService;

  @Value("${unihealth.app.users.delete.unverified.after.days}")
  private int deleteAfterDays;

  @Override
  public int deleteAfterRegistrationExpiry() {
    List<User> unverifiedUsers =
        userRepository.findByStatusAndLastLoginIsNull(UserStatus.UNVERIFIED);

    LocalDate deletionTargetDate = LocalDate.now().minusDays(deleteAfterDays);
    List<User> usersToDelete = new ArrayList<>();

    List<User> reactivatedUsers =
        unverifiedUsers.stream().filter(u -> u.getReactivatedOn() != null).toList();
    List<User> notReactivatedUsers =
        unverifiedUsers.stream().filter(u -> u.getReactivatedOn() == null).toList();
    for (User unverifiedUser : notReactivatedUsers) {
      UserRepresentation unverifiedUserRepresentation =
          keycloakAdminClient.getUserRepresentationByUsername(unverifiedUser.getUsername());
      LocalDate creationDate =
          getDateFromUserCreationTimestamp(unverifiedUserRepresentation.getCreatedTimestamp());
      if (!deletionTargetDate.isBefore(creationDate)) {
        usersToDelete.add(unverifiedUser);
      }
    }

    for (User unverifiedReactivatedUser : reactivatedUsers) {
      if (!deletionTargetDate.isBefore(
          unverifiedReactivatedUser.getReactivatedOn().toLocalDate())) {
        usersToDelete.add(unverifiedReactivatedUser);
      }
    }

    // Drop the accounts delete() refuses anyway, so one protected user does not abort the batch.
    usersToDelete.removeIf(user -> {
      if (user.getUsername().equals(tenantContext.getCurrentTenantAdminUsername())) {
        log.warn("{} is the deployment administrator and cannot be deleted", user.getUsername());
        return true;
      }
      if (userReaderService.isLastAdmin(user.getId())) {
        log.warn("{} is the last administrator and cannot be deleted", user.getUsername());
        return true;
      }
      return false;
    });

    for (User userToDelete : usersToDelete) {
      userService.delete(userToDelete.getId());
    }

    return usersToDelete.size();
  }

  private LocalDate getDateFromUserCreationTimestamp(long userCreationTimestamp) {
    return DateUtils.toLocalDate(userCreationTimestamp);
  }
}
