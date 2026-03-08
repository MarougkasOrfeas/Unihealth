package gr.uniwa.unihealth.backend.service.impl;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.dto.RssFeedDTO;
import gr.uniwa.unihealth.backend.mapper.BaseUpdatableEntityMapper;
import gr.uniwa.unihealth.backend.mapper.RssFeedMapper;
import gr.uniwa.unihealth.backend.model.RssFeed;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.RssFeedRepository;
import gr.uniwa.unihealth.backend.service.RssFeedReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RssFeedReaderServiceImpl extends BaseUpdatableServiceImpl<RssFeedDTO, RssFeed>
    implements RssFeedReaderService {

  private final RssFeedMapper mapper;
  private final RssFeedRepository repository;

  @Override
  public RssFeedDTO findById(String id) {
    return null;
  }

  @Override
  public Page<RssFeedDTO> findAll(Predicate predicate, Pageable pageable) {
    return null;
  }

  @Override
  public List<RssFeedDTO> findAll() {
    return List.of();
  }

  @Override
  protected BaseReaderServiceImpl<RssFeedDTO, RssFeed> getReaderService() {
    return null;
  }

  @Override
  protected BaseUpdatableEntityMapper<RssFeedDTO, RssFeed> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<RssFeed> getRepository() {
    return repository;
  }
}
