package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.config.properties.RssSourceProperties;
import gr.uniwa.unihealth.backend.dto.RssFeedDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.RssFeedMapper;
import gr.uniwa.unihealth.backend.model.RssFeed;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.RssFeedRepository;
import gr.uniwa.unihealth.backend.service.RssFeedReaderService;
import gr.uniwa.unihealth.backend.service.client.RssClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssFeedReaderServiceImpl extends BaseReaderServiceImpl<RssFeedDTO, RssFeed>
    implements RssFeedReaderService {

  private final RssFeedMapper mapper;
  private final RssFeedRepository repository;
  private final RssClient rssClient;
  private final RssSourceProperties rssSourceProperties;
  private final TenantContext tenantContext;

  @PostConstruct
  public void init() {
    tenantContext.runForEachTenant(tenantId -> {
      if (repository.count() == 0) {
        log.info("Initializing RSS feeds for tenant [{}].", tenantId);
        refreshFeeds();
      } else {
        log.info("RSS feeds already exist for tenant [{}]. Skipping initialization.", tenantId);
      }
    }, "RssFeedReaderServiceImpl.init");
  }

  @Override
  public List<RssFeedDTO> refreshFeeds() {
    List<RssFeed> feedsToSave = new ArrayList<>();

    for (RssSourceProperties.Source source : rssSourceProperties.getSources()) {
      List<RssFeed> fetchedFeeds = rssClient.fetch(source);

      for (RssFeed feed : fetchedFeeds) {
        if (feed.getLink() != null && !repository.existsByLink(feed.getLink())) {
          feedsToSave.add(feed);
        }
      }
    }

    List<RssFeed> savedFeeds = repository.saveAll(feedsToSave);

    return savedFeeds.stream().map(mapper::mapToDTO).toList();
  }

  @Override
  public List<RssFeedDTO> findRelevantFeeds(int limit) {
    Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
    return repository.findAllByOrderByPublishedDateDesc(pageable).getContent().stream()
        .map(mapper::mapToDTO).toList();
  }

  @Override
  protected BaseEntityMapper<RssFeedDTO, RssFeed> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<RssFeed> getRepository() {
    return repository;
  }
}
