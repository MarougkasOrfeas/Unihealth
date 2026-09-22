package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.UserSurveyDTO;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.UserSurvey;
import gr.uniwa.unihealth.backend.mapper.UserSurveyMapper;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.repository.UserSurveyRepository;
import gr.uniwa.unihealth.backend.service.UserSurveyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and upserts one student's survey response.
 *
 * <p>A plain {@code @Service} rather than an extension of {@code BaseServiceImpl}: that base would
 * demand four overrides this slice never calls, and the newer per-user services in this application
 * are all written this way. The MapStruct mapper is injected rather than inherited, which is the
 * only part of the base worth having here.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserSurveyServiceImpl implements UserSurveyService {

  private final UserSurveyRepository repository;
  private final UserRepository userRepository;
  private final UserSurveyMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public UserSurveyDTO findForUser(String username) {
    return repository.findByUserUsername(username)
        .map(mapper::mapToDTO)
        .orElseGet(UserSurveyDTO::new);
  }

  /**
   * Find-or-create, then overwrite every answer in place.
   *
   * <p>This races if the same student submits twice at once; {@code uq_user_survey_user} is the real
   * guard, and the losing request fails rather than silently creating a second response. Not worth
   * locking for: the two submissions would be saying the same thing.
   */
  @Override
  public void saveForUser(String username, UserSurveyDTO dto) {
    UserSurvey entity = repository.findByUserUsername(username).orElseGet(() -> {
      UserSurvey created = new UserSurvey();
      created.setUser(loadUser(username));
      return created;
    });

    boolean isFirstResponse = entity.getId() == null;

    mapper.mapForUpdate(dto, entity);
    repository.save(entity);

    // No answer text in the log. The counts are enough to see the survey is being used, and the
    // free-text box is somewhere a student may well write something about their health.
    log.info("Survey response {} for a user", isFirstResponse ? "recorded" : "updated");
  }

  private User loadUser(String username) {
    return userRepository.findByUsername(username).orElseThrow(
        () -> new EntityNotFoundException("No user found for the authenticated principal."));
  }
}
