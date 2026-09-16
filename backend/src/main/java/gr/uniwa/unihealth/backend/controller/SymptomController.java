package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.dto.PossibleCauseDTO;
import gr.uniwa.unihealth.backend.dto.SymptomFactorDTO;
import gr.uniwa.unihealth.backend.dto.SymptomItemDTO;
import gr.uniwa.unihealth.backend.dto.SymptomItemDetailDTO;
import gr.uniwa.unihealth.backend.service.PossibleCauseService;
import gr.uniwa.unihealth.backend.service.SymptomItemReaderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("symptoms")
@RequiredArgsConstructor
public class SymptomController {

  private final SymptomItemReaderService service;
  private final PossibleCauseService possibleCauseService;

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

  @Operation(summary = "Find the factors of a symptom",
      description = "Returns the details a reader can tick to narrow a symptom down. Empty for "
          + "symptoms the source does not break down into possible causes.")
  @PostMapping("_factors")
  public List<SymptomFactorDTO> findFactors(@RequestBody Map<String, Object> requestBody) {

    String slug = Objects.toString(requestBody.get("slug"), "").trim();
    return possibleCauseService.findFactors(slug);
  }

  @Operation(summary = "Find the possible causes of a symptom",
      description = "Ranks the published causes of a symptom by how much of each one the reader "
          + "recognised. Causes that matched nothing are returned last, not dropped. This is not a "
          + "diagnosis.")
  @PostMapping("_possible_causes")
  public List<PossibleCauseDTO> findPossibleCauses(@RequestBody Map<String, Object> requestBody) {

    String slug = Objects.toString(requestBody.get("slug"), "").trim();
    return possibleCauseService.findPossibleCauses(slug, readFactorCodes(requestBody));
  }

  private List<String> readFactorCodes(Map<String, Object> requestBody) {
    Object raw = requestBody.get("factorCodes");
    if (!(raw instanceof List<?> values)) {
      return List.of();
    }

    List<String> codes = new ArrayList<>();
    for (Object value : values) {
      String code = Objects.toString(value, "").trim();
      if (!code.isEmpty()) {
        codes.add(code);
      }
    }
    return codes;
  }
}
