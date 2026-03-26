package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.dto.HealthProfileViewDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.HealthProfileMapper;
import gr.uniwa.unihealth.backend.model.HealthProfile;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.HealthProfileRepository;
import gr.uniwa.unihealth.backend.service.HealthProfileReaderService;
import gr.uniwa.unihealth.backend.utils.profile.HealthProfileCalculationUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthProfileReaderServiceImpl
    extends BaseReaderServiceImpl<HealthProfileDTO, HealthProfile>
    implements HealthProfileReaderService {

  private final HealthProfileMapper mapper;
  private final HealthProfileRepository repository;

  @Override
  public HealthProfileViewDTO findByCurrentUser(String username) {
    HealthProfile entity = repository.findByUserUsername(username).orElseThrow(
        () -> new EntityNotFoundException("Health profile not found for current user."));

    return toViewDto(entity);
  }

  public HealthProfileViewDTO toViewDto(HealthProfile entity) {
    HealthProfileViewDTO dto = mapper.mapToViewDTO(entity);
    dto.setAge(HealthProfileCalculationUtils.calculateAge(dto.getDateOfBirth()));
    dto.setBmi(HealthProfileCalculationUtils.calculateBmi(dto.getHeightCm(), dto.getWeightKg()));
    return dto;
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
