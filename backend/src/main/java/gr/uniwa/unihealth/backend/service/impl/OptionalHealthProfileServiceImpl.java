package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.OptionalHealthProfileDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.OptionalHealthProfileMapper;
import gr.uniwa.unihealth.backend.model.HealthProfile;
import gr.uniwa.unihealth.backend.model.OptionalHealthProfile;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.HealthProfileRepository;
import gr.uniwa.unihealth.backend.repository.OptionalHealthProfileRepository;
import gr.uniwa.unihealth.backend.service.OptionalHealthProfileService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OptionalHealthProfileServiceImpl
    extends BaseServiceImpl<OptionalHealthProfileDTO, OptionalHealthProfile>
    implements OptionalHealthProfileService {

  private final OptionalHealthProfileRepository repository;
  private final HealthProfileRepository healthProfileRepository;
  private final OptionalHealthProfileMapper mapper;
  private final OptionalHealthProfileReaderServiceImpl service;

  @Override
  public void updateCurrentUserOptionalProfile(String username, OptionalHealthProfileDTO dto) {
    HealthProfile healthProfile = healthProfileRepository.findByUserUsername(username).orElseThrow(
        () -> new EntityNotFoundException("Health profile not found for current user."));

    OptionalHealthProfile entity =
        repository.findByHealthProfileId(healthProfile.getId()).orElseGet(() -> {
          OptionalHealthProfile newEntity = new OptionalHealthProfile();
          newEntity.setHealthProfile(healthProfile);
          return newEntity;
        });

    mapper.mapForUpdate(dto, entity);

    normalizeOptionalFields(entity);

    repository.save(entity);
  }

  private void normalizeOptionalFields(OptionalHealthProfile entity) {
    if (Boolean.FALSE.equals(entity.getMedication())) {
      entity.setMedicationDetails(null);
    }

    if (entity.getMedicationDetails() != null && entity.getMedicationDetails().isBlank()) {
      entity.setMedicationDetails(null);
    }

    if (Boolean.FALSE.equals(entity.getSurgeryHistory())) {
      entity.setSurgeryDetails(null);
    }

    if (entity.getSurgeryDetails() != null && entity.getSurgeryDetails().isBlank()) {
      entity.setSurgeryDetails(null);
    }

    if (entity.getComments() != null && entity.getComments().isBlank()) {
      entity.setComments(null);
    }
  }

  @Override
  protected BaseReaderServiceImpl<OptionalHealthProfileDTO, OptionalHealthProfile> getReaderService() {
    return service;
  }

  @Override
  protected BaseEntityMapper<OptionalHealthProfileDTO, OptionalHealthProfile> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<OptionalHealthProfile> getRepository() {
    return repository;
  }
}
