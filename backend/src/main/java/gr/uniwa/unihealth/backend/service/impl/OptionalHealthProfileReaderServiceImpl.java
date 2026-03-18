package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.OptionalHealthProfileDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.OptionalHealthProfileMapper;
import gr.uniwa.unihealth.backend.model.OptionalHealthProfile;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.OptionalHealthProfileRepository;
import gr.uniwa.unihealth.backend.service.OptionalHealthProfileReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OptionalHealthProfileReaderServiceImpl extends BaseReaderServiceImpl<OptionalHealthProfileDTO, OptionalHealthProfile> implements
    OptionalHealthProfileReaderService {

  private final OptionalHealthProfileMapper mapper;
  private final OptionalHealthProfileRepository repository;

  @Override
  public OptionalHealthProfileDTO findByCurrentUser(String username) {
    return repository.findByHealthProfileUserUsername(username)
        .map(mapper::mapToDTO)
        .orElseGet(OptionalHealthProfileDTO::new);
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
