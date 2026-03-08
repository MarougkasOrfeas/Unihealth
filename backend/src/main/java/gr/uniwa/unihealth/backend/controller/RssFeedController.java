package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.dto.RssFeedDTO;
import gr.uniwa.unihealth.backend.service.RssFeedReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("rss")
@RequiredArgsConstructor
public class RssFeedController {

  private RssFeedReaderService readerService;

  @PostMapping
  public List<RssFeedDTO> getLatestRssFeeds(
      @RequestBody(required = false) Map<String, Object> requestBody) {
    //return readerService.getLatestFeeds(requestBody);
    return List.of();
  }
}
