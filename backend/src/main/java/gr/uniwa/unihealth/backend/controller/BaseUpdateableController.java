package gr.uniwa.unihealth.backend.controller;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.BaseUpdatableDTO;
import gr.uniwa.unihealth.backend.dto.ExcelDownloadResponseDTO;
import gr.uniwa.unihealth.backend.service.BaseUpdatableService;
import gr.uniwa.unihealth.backend.service.export.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

public abstract class BaseUpdateableController<D extends BaseUpdatableDTO>
    extends BaseController<D> {

  @Operation(summary = "Updates an existing resource",
      description = "Updates an existing resource in the application database.")
  @PutMapping("{id}")
  public void update(@PathVariable String id, @RequestBody @Valid D dto) {
    BaseUpdatableService<D> service = getService();
    if (service == null) {
      throw new QDoesNotExistException();
    }
    service.update(id, dto);
  }

  @Operation(summary = "Exports resources to Excel from provided data",
      description = "Generates an Excel file from the provided resource export data. Allows client-side filtering/sorting for computed fields.")
  @PostMapping("_export")
  public ExcelDownloadResponseDTO export(
      @RequestBody(required = false) Map<String, Object> requestBody) {
    ExportService<D> exportService = getExportService();
    if (exportService == null) {
      throw new QDoesNotExistException();
    }
    return exportService.exportToExcel(requestBody);
  }

  protected abstract BaseUpdatableService<D> getService();

  protected abstract ExportService<D> getExportService();
}
