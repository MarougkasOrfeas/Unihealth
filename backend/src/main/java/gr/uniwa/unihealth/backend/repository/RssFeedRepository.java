package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.RssFeed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;


public interface RssFeedRepository extends BaseRepository<RssFeed> {

  boolean existsByLink(String link);
  
  boolean existsByCreatedOnGreaterThanEqual(LocalDateTime createdOn);

  Page<RssFeed> findAllByOrderByPublishedDateDesc(Pageable pageable);
}
