package gr.uniwa.unihealth.backend.service.export;

import gr.uniwa.unihealth.backend.dto.BaseUpdatableDTO;
import gr.uniwa.unihealth.backend.dto.ExcelDownloadResponseDTO;

import java.util.Map;

public interface ExportService<D extends BaseUpdatableDTO> {

  /**
   * Exports data to an Excel file based on the provided parameters, which may include filters,
   * sorting, and pagination options. The method retrieves the relevant data using the reader
   * service, generates an Excel file, and returns it wrapped in an ExcelDownloadResponseDTO for
   * download.
   *
   * @param parameters The parameters defining filters, sorting, and pagination for the data to be
   *                   exported.
   * @return An ExcelDownloadResponseDTO containing the generated Excel file and metadata for
   * download.
   */
  ExcelDownloadResponseDTO exportToExcel(Map<String, Object> parameters);

  long getExportDataCount(Map<String, Object> parameters);
}
