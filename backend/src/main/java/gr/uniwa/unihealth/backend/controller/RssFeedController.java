package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.dto.RssFeedDTO;
import gr.uniwa.unihealth.backend.model.enums.Permission;
import gr.uniwa.unihealth.backend.service.BaseReaderService;
import gr.uniwa.unihealth.backend.service.BaseService;
import gr.uniwa.unihealth.backend.service.RssFeedReaderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("rss")
@RequiredArgsConstructor
public class RssFeedController extends BaseController<RssFeedDTO> {

  private final RssFeedReaderService service;

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


  @Override
  protected BaseService<RssFeedDTO> getService() {
    return null;
  }

  @Override
  protected BaseReaderService<RssFeedDTO> getReaderService() {
    return service;
  }

  @Override
  protected Collection<Permission> getReadPermissions() {
    return List.of();
  }
}
