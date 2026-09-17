package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.querydsl.core.types.dsl.BooleanExpression;
import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.mapper.UserMapper;
import gr.uniwa.unihealth.backend.model.QUser;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.UserRoles;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.BaseReaderWithSearchService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementation for {@link UserReaderService}.
 *
 * @author omaro
 */
@Service
@RequiredArgsConstructor
public class UserReaderServiceImpl extends BaseReaderServiceImpl<UserDTO, User>
    implements UserReaderService, BaseReaderWithSearchService<UserDTO> {

  private final UserMapper mapper;
  private final UserRepository repository;
  private final AuthenticationContext authenticationContext;

  /**
   * Free-text search over the columns the list screen actually shows. Without this,
   * {@link BaseReaderServiceImpl#createPredicateFromParams} drops the {@code search} parameter
   * entirely, because it only applies it to readers implementing
   * {@link BaseReaderWithSearchService}.
   */
  @Override
  public BooleanExpression buildSearchPredicate(BuildSearchPredicateParams params) {
    String search = params.search();
    if (search == null || search.isBlank()) {
      return null;
    }

    // Own columns only. Traversing `department.name` here would emit an implicit inner join and
    // silently drop every user without a department from the results; use the Department facet
    // to filter on that instead.
    QUser user = QUser.user;
    return user.username.containsIgnoreCase(search)
        .or(user.firstname.containsIgnoreCase(search))
        .or(user.lastname.containsIgnoreCase(search))
        .or(user.email.containsIgnoreCase(search));
  }

  @Override
  public UserDTO findLoggedInUser() {
    return repository.findByUsername(authenticationContext.getCurrentUsername())
        .map(mapper::mapToDTO)
        .orElseThrow(() -> new QDoesNotExistException("Could not find logged in user."));
  }

  @Override
  public boolean isHealthProfileCompleted() {
    return repository.findHealthProfileCompletedByUsername(
            authenticationContext.getCurrentUsername())
        .orElseThrow(() -> new QDoesNotExistException("Could not find logged in user."));
  }

  @Override
  public boolean isLastAdmin(String userId) {
    return repository.findById(userId)
        .filter(user -> UserRoles.ADMIN.equals(user.getRole()))
        // Only active administrators count as a replacement: a deactivated one cannot sign in,
        // which is the same as not having one, per RightsMatrixResolver.
        .map(user -> repository.countByRoleAndStatusAndIdNot(
            UserRoles.ADMIN, UserStatus.ACTIVE, userId) == 0)
        .orElse(false);
  }

  @Override
  protected UserMapper getMapper() {
    return mapper;
  }

  @Override
  protected UserRepository getRepository() {
    return repository;
  }
}
