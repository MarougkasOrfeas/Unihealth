package gr.uniwa.unihealth.backend.service.impl;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.config.properties.RssSourceProperties;
import gr.uniwa.unihealth.backend.dto.RssFeedDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.RssFeedMapper;
import gr.uniwa.unihealth.backend.model.QRssFeed;
import gr.uniwa.unihealth.backend.model.RssFeed;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.RssFeedRepository;
import gr.uniwa.unihealth.backend.service.RssFeedReaderService;
import gr.uniwa.unihealth.backend.service.client.RssClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
      if (!repository.existsByCreatedOnGreaterThanEqual(LocalDate.now().atStartOfDay())) {
        log.info("No RSS feeds fetched today for tenant [{}]. Refreshing.", tenantId);
        refreshFeeds();
      } else {
        log.info("RSS feeds already fetched today for tenant [{}]. Skipping.", tenantId);
      }
    }, "RssFeedReaderServiceImpl.init");
  }

  @Override
  public Page<RssFeedDTO> findAll(Predicate predicate, Pageable pageable) {
    BooleanExpression lastSevenDaysPredicate =
        QRssFeed.rssFeed.createdOn.goe(LocalDateTime.now().minusDays(30));

    Predicate finalPredicate =
        predicate == null ? lastSevenDaysPredicate : lastSevenDaysPredicate.and(predicate);

    return repository.findAll(finalPredicate, pageable).map(mapper::mapToDTO);
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
