package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.dto.DataSourceDTO;
import gr.uniwa.unihealth.backend.service.DataSourceReaderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("sources")
@RequiredArgsConstructor
public class DataSourceController {

  private final DataSourceReaderService service;

  @Operation(summary = "Find data sources",
      description = "Returns the external sources the content was ingested from, with their "
          + "licences, for the credits shown alongside it.")
  @PostMapping("_content")
  public List<DataSourceDTO> findContent() {
    return service.findContent();
  }
}
