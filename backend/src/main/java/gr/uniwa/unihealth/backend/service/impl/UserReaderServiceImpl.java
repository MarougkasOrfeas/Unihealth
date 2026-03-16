package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.mapper.UserMapper;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.repository.UserRepository;
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
    implements UserReaderService {

  private final UserMapper mapper;
  private final UserRepository repository;
  private final AuthenticationContext authenticationContext;

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
  protected UserMapper getMapper() {
    return mapper;
  }

  @Override
  protected UserRepository getRepository() {
    return repository;
  }
}
