package gr.uniwa.unihealth.backend.controller;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.controller.util.ControllerUtils;
import gr.uniwa.unihealth.backend.dto.ConditionDTO;
import gr.uniwa.unihealth.backend.dto.ConditionDetailDTO;
import gr.uniwa.unihealth.backend.model.Condition;
import gr.uniwa.unihealth.backend.service.ConditionReaderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("conditions")
@RequiredArgsConstructor
public class ConditionController {

  private final ConditionReaderService service;
  private final ControllerUtils controllerUtils;

  @Operation(summary = "Find conditions",
      description = "Returns active conditions ordered alphabetically. Supports global search.")
  @PostMapping("_content")
  public List<ConditionDTO> findContent(
      @RequestBody(required = false) Map<String, Object> requestBody) {

    String search =
        requestBody != null ? Objects.toString(requestBody.get("search"), "").trim() : "";

    return service.findContent(search);
  }

  @Operation(summary = "Find condition by slug",
      description = "Returns a condition with the symptoms that lead to it.")
  @PostMapping("_by_slug")
  public ConditionDetailDTO findBySlug(@RequestBody Map<String, Object> requestBody) {

    String slug = Objects.toString(requestBody.get("slug"), "").trim();
    return service.findBySlug(slug);
  }

  @Operation(summary = "Find conditions page", description = "Returns a page of conditions.")
  @PostMapping("_page")
  public Page<ConditionDTO> findPage(@RequestBody Map<String, Object> requestBody) {

    Map.Entry<Predicate, Pageable> predicateAndPageable =
        controllerUtils.getPredicateAndPageable(requestBody, Condition.class);

    return service.findAll(predicateAndPageable.getKey(), predicateAndPageable.getValue());
  }
}
