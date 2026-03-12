package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.dto.UnihealthAssistantContentDTO;
import gr.uniwa.unihealth.backend.service.UnihealthAssistantReaderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("assistant")
@RequiredArgsConstructor
public class UnihealthAssistantController {

  private final UnihealthAssistantReaderService service;

  @Operation(summary = "Find assistant content",
      description = "Returns assistant content grouped by section.")
  @PostMapping("_content")
  public UnihealthAssistantContentDTO findContent(
      @RequestBody(required = false) Map<String, Object> requestBody) {
    String search =
        requestBody != null ? String.valueOf(requestBody.getOrDefault("search", "")).trim() : "";

    if (!search.isBlank()) {
      return service.searchContent(search);
    }

    return service.findContent();
  }
}
