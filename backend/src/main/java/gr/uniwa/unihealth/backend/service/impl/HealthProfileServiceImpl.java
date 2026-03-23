package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.HealthProfileMapper;
import gr.uniwa.unihealth.backend.model.HealthProfile;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.HealthProfileRepository;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.HealthProfileService;
import gr.uniwa.unihealth.backend.service.personalization.LabelEvaluatorService;
import gr.uniwa.unihealth.backend.service.personalization.UserProfileLabelsService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthProfileServiceImpl extends BaseServiceImpl<HealthProfileDTO, HealthProfile>
    implements HealthProfileService {

  private final HealthProfileRepository repository;
  private final UserRepository userRepository;
  private final HealthProfileMapper mapper;
  private final HealthProfileReaderServiceImpl service;
  private final LabelEvaluatorService labelEvaluatorService;
  private final UserProfileLabelsService userProfileLabelsService;


  @Override
  public String completeProfile(String username, HealthProfileDTO dto) {
    validateForCreate(dto);

    if (repository.existsByUserUsername(username)) {
      throw new IllegalStateException("Health profile already completed for current user.");
    }

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("User not found for username: " + username));

    HealthProfile entity = mapper.mapForCreate(dto);
    entity.setUser(user);

    HealthProfile saved = repository.save(entity);

    user.setHealthProfileCompleted(true);
    user.setHealthProfileCompletedOn(LocalDateTime.now());
    userRepository.save(user);

    List<String> sortedLabels = labelEvaluatorService.evaluateAndSort(dto);
    userProfileLabelsService.saveForUser(user, sortedLabels);

    return saved.getId();
  }

  @Override
  public HealthProfileDTO findByCurrentUser(String username) {
    HealthProfile entity = repository.findByUserUsername(username).orElseThrow(
        () -> new EntityNotFoundException("Health profile not found for current user."));

    return mapper.mapToDTO(entity);
  }

  @Override
  public void updateCurrentUserProfile(String username, HealthProfileDTO dto) {
    HealthProfile entity = repository.findByUserUsername(username).orElseThrow(
        () -> new EntityNotFoundException("Health profile not found for current user."));

    validate(entity.getId(), dto);

    mapper.mapForUpdate(dto, entity);
    repository.save(entity);
  }


  @Override
  protected BaseReaderServiceImpl<HealthProfileDTO, HealthProfile> getReaderService() {
    return service;
  }

  @Override
  protected BaseEntityMapper<HealthProfileDTO, HealthProfile> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<HealthProfile> getRepository() {
    return repository;
  }
}
