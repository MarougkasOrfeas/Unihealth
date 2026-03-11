package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.RssFeed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface RssFeedRepository extends BaseRepository<RssFeed> {

  boolean existsByLink(String link);

  Page<RssFeed> findAllByOrderByPublishedDateDesc(Pageable pageable);
}
