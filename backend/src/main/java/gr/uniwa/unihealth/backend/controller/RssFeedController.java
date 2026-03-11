package gr.uniwa.unihealth.backend.controller;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.controller.util.ControllerUtils;
import gr.uniwa.unihealth.backend.dto.RssFeedDTO;
import gr.uniwa.unihealth.backend.model.RssFeed;
import gr.uniwa.unihealth.backend.service.RssFeedReaderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  private final RssFeedReaderService service;
  private final ControllerUtils controllerUtils;

  @Operation(summary = "Finds RSS feeds",
      description = "Returns RSS feed items with pagination information.")
  @PostMapping("_page")
  public Page<RssFeedDTO> findPage(@RequestBody(required = false) Map<String, Object> requestBody) {

    Map.Entry<Predicate, Pageable> predicateAndPageable =
        controllerUtils.getPredicateAndPageable(requestBody, RssFeed.class);

    Predicate predicateToUse = predicateAndPageable.getKey();

    return service.findAll(predicateToUse, predicateAndPageable.getValue());
  }

  @Operation(summary = "Refresh RSS feeds",
      description = "Fetches latest RSS feeds from configured sources and stores them.")
  @PostMapping("refresh")
  public List<RssFeedDTO> refreshFeeds() {
    return service.refreshFeeds();
  }

  @Operation(summary = "Find relevant RSS feeds for home page",
      description = "Returns the latest and most relevant RSS feed items.")
  @PostMapping("_home")
  public List<RssFeedDTO> findRelevantFeeds(
      @RequestBody(required = false) Map<String, Object> requestBody) {
    int limit = 10;

    if (requestBody != null && requestBody.get("limit") != null) {
      limit = Integer.parseInt(requestBody.get("limit").toString());
    }

    return service.findRelevantFeeds(limit);
  }
}
