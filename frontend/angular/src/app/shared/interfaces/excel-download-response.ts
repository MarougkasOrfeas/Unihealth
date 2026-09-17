/**
 * DTO for Excel file download responses from the backend.
 * Contains the Excel file data along with metadata.
 * Note: Jackson automatically serializes byte[] as Base64 string in JSON.
 */
export interface ExcelDownloadResponseDTO {
  /**
   * The Excel file content (Base64-encoded by Jackson when serializing byte[]).
   */
  data: string;

  /**
   * The suggested filename for the download (e.g., "Users_NW_20240115_143022.xlsx").
   */
  filename: string;

  /**
   * The MIME type of the file (e.g., "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").
   */
  contentType: string;
}
