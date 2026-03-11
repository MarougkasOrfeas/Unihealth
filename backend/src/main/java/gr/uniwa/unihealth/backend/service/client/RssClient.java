package gr.uniwa.unihealth.backend.service.client;

import gr.uniwa.unihealth.backend.config.properties.RssSourceProperties;
import gr.uniwa.unihealth.backend.model.RssFeed;

import java.util.List;

public interface RssClient {

  List<RssFeed> fetch(RssSourceProperties.Source source);
}
