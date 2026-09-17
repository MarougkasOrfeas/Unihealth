package gr.uniwa.unihealth.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for Excel file download responses. Contains the Excel file as Base64-encoded data along with
 * the filename. This approach avoids relying on Content-Disposition headers which can be
 * problematic with certain proxies or CORS configurations.
 *
 * @author omaro
 */
@Getter
@Setter
@Builder
public class ExcelDownloadResponseDTO {

  /**
   * The Excel file content encoded as Base64 string.
   */
  private byte[] data;

  /**
   * The suggested filename for the download (e.g., "Users_NW_20240115_143022.xlsx").
   */
  private String filename;

  /**
   * The MIME type of the file (e.g.,
   * "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").
   */
  private String contentType;
}
