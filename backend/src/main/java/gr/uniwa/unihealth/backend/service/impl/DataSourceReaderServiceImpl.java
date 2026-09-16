package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.DataSourceDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.DataSourceMapper;
import gr.uniwa.unihealth.backend.model.DataSource;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.DataSourceRepository;
import gr.uniwa.unihealth.backend.service.DataSourceReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataSourceReaderServiceImpl extends BaseReaderServiceImpl<DataSourceDTO, DataSource>
    implements DataSourceReaderService {

  private final DataSourceRepository repository;
  private final DataSourceMapper mapper;

  @Override
  public List<DataSourceDTO> findContent() {
    return repository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
        .map(mapper::mapToDTO)
        .toList();
  }

  @Override
  protected BaseEntityMapper<DataSourceDTO, DataSource> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<DataSource> getRepository() {
    return repository;
  }
}
