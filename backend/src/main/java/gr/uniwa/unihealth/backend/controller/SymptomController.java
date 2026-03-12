package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.dto.SymptomItemDTO;
import gr.uniwa.unihealth.backend.dto.SymptomItemDetailDTO;
import gr.uniwa.unihealth.backend.service.SymptomItemReaderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("symptoms")
@RequiredArgsConstructor
public class SymptomController {

  private final SymptomItemReaderService service;

  @Operation(summary = "Find symptoms",
      description = "Returns active symptoms ordered alphabetically. Supports global search.")
  @PostMapping("_content")
  public List<SymptomItemDTO> findContent(
      @RequestBody(required = false) Map<String, Object> requestBody) {

    String search =
        requestBody != null ? Objects.toString(requestBody.get("search"), "").trim() : "";

    return service.findContent(search);
  }

  @Operation(summary = "Find symptom by slug",
      description = "Returns full symptom detail content by slug.")
  @PostMapping("_by_slug")
  public SymptomItemDetailDTO findBySlug(@RequestBody Map<String, Object> requestBody) {

    String slug = Objects.toString(requestBody.get("slug"), "").trim();
    return service.findBySlug(slug);
  }
}
