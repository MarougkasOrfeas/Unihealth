package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.RssFeedDTO;

import java.util.List;

public interface RssFeedReaderService extends BaseReaderService<RssFeedDTO> {

  List<RssFeedDTO> refreshFeeds();

  List<RssFeedDTO> findRelevantFeeds(int limit);

}
