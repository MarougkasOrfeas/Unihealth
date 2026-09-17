package gr.uniwa.unihealth.backend.service.impl;


import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.dto.BaseUpdatableDTO;
import gr.uniwa.unihealth.backend.dto.ExcelDownloadResponseDTO;
import gr.uniwa.unihealth.backend.exception.UNIHEALTHException;
import gr.uniwa.unihealth.backend.service.BaseReaderService;
import gr.uniwa.unihealth.backend.service.export.ExportService;
import gr.uniwa.unihealth.backend.service.TranslationReaderService;
import gr.uniwa.unihealth.backend.service.permission.UserPermissionService;
import gr.uniwa.unihealth.backend.service.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Transactional(readOnly = true)
public abstract class ExportServiceImpl<D extends BaseUpdatableDTO> implements ExportService<D> {

  protected static final DateTimeFormatter TIMESTAMP_FORMATTER = DateUtils.TIMESTAMP_FORMATTER;

  private static final String ACTIVE_STATUS_KEY = "global.status.activated";

  private static final String INACTIVE_STATUS_KEY = "global.status.deactivated";

  protected final Map<String, String> columns;

  protected final TenantContext tenantContext;

  protected final String[] columnHeaders;

  protected final List<String> defaultColumns;

  protected final DateTimeFormatter dateTimeFormatter;

  protected final DateTimeFormatter dateFormatter;

  protected final TranslationReaderService translationReaderService;

  protected final BaseReaderService<D> readerService;

  protected final UserPermissionService userPermissionService;

  protected final ThreadLocal<List<ExportColumn>> exportColumns = new ThreadLocal<>();


  /**
   * Default constructor for tables that do not use the saved column config impl
   **/

  protected ExportServiceImpl(TenantContext tenantContext,
      TranslationReaderService translationReaderService, String[] columnHeaders,
      DateTimeFormatter dateTimeFormatter, DateTimeFormatter dateFormatter,
      BaseReaderService<D> readerService, UserPermissionService userPermissionService) {
    this.tenantContext = tenantContext;
    this.translationReaderService = translationReaderService;
    this.columnHeaders = columnHeaders;
    this.defaultColumns = null;
    this.columns = null;
    this.dateTimeFormatter = dateTimeFormatter;
    this.dateFormatter = dateFormatter;
    this.readerService = readerService;
    this.userPermissionService = userPermissionService;
  }

  protected ExportServiceImpl(TenantContext tenantContext,
      TranslationReaderService translationReaderService, Map<String, String> columns,
      List<String> defaultColumns, DateTimeFormatter dateTimeFormatter,
      DateTimeFormatter dateFormatter, BaseReaderService<D> readerService,
      UserPermissionService userPermissionService) {
    this.tenantContext = tenantContext;
    this.translationReaderService = translationReaderService;
    this.columnHeaders = null;
    this.columns = columns;
    this.defaultColumns = defaultColumns;
    this.dateTimeFormatter = dateTimeFormatter;
    this.dateFormatter = dateFormatter;
    this.readerService = readerService;
    this.userPermissionService = userPermissionService;
    overrideInheritedFieldTranslations(this.columns);
  }

  /**
   * Creates a data row for a single user.
   */
  protected abstract void createDataRow(Sheet sheet, int rowNum, D dto, CellStyle dataStyle,
      String locale);

  /**
   * Gets the filename for the export. Format: Users_TenantName_timestamp
   *
   * @param locale the locale to use for translations
   * @return The generated filename with .xlsx extension.
   */
  protected abstract String getExportFilename(String locale);

  /**
   * Gets the sheet name for the excel export
   *
   * @param locale the locale to use for translations
   * @return the translated sheet name
   */
  protected abstract String getSheetName(String locale);

  @Override
  public ExcelDownloadResponseDTO exportToExcel(Map<String, Object> parameters) {
    String locale =
        parameters != null ? Objects.toString(parameters.get("locale"), "el").trim() : "el";

    List<D> exportData = fetchDataForExport(parameters);

    return createExcelDownloadResponse(exportToExcel(exportData, locale),
        getExportFilename(locale));
  }


  @Override
  public long getExportDataCount(Map<String, Object> parameters) {
    return fetchDataForExport(parameters).size();
  }

  protected List<D> fetchDataForExport(Map<String, Object> parameters) {
    return readerService != null ? readerService.findAll(parameters).getContent() : List.of();
  }

  private byte[] exportToExcel(List<D> exportData, String locale) {
    log.info("Received export request with {} records", exportData.size());

    String currentTenant = tenantContext.getCurrentTenant();
    log.info("Starting export for tenant: {} with {} records", currentTenant, exportData.size());

    try (ByteArrayOutputStream excelData = generateExcel(exportData, locale)) {
      log.info("Export completed successfully");
      return excelData.toByteArray();
    } catch (IOException e) {
      throw new UNIHEALTHException(null, "Failed to generate Excel export", e);
    }
  }

  protected ExcelDownloadResponseDTO createExcelDownloadResponse(byte[] excelData,
      String filename) {
    return ExcelDownloadResponseDTO.builder().data(excelData).filename(filename)
        .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").build();
  }

  /**
   * Translates provided columnHeader translation codes
   *
   * @param locale uses locale
   * @return array with translations
   */
  private String[] getTranslatedHeaders(String locale) {
    return Arrays.stream(columnHeaders).map(key -> translationReaderService.translate(key, locale))
        .toArray(String[]::new);
  }


  /**
   * Generates the Excel file from the export data.
   */
  private ByteArrayOutputStream generateExcel(List<D> exportData, String locale) {
    try (Workbook workbook = new XSSFWorkbook()) {
      String sheetName = getSheetName(locale);
      Sheet sheet = workbook.createSheet(sheetName);

      // Create styles
      CellStyle headerStyle = createHeaderStyle(workbook);
      CellStyle dataStyle = createDataStyle(workbook);

      // Create data rows
      int rowNum = 1;
      for (D dto : exportData) {
        createDataRow(sheet, rowNum++, dto, dataStyle, locale);
      }

      // Write to output stream
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      workbook.write(outputStream);
      return outputStream;

    } catch (IOException e) {
      log.error("Error generating Excel file for user export", e);
      throw new UNIHEALTHException(null, "Failed to generate Excel export", e);
    } finally {
      exportColumns.remove();
    }
  }

  /**
   * Creates the header style for the Excel sheet.
   */
  public CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();

    // Background color
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    // Font
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 11);
    style.setFont(font);

    // Alignment
    style.setAlignment(HorizontalAlignment.LEFT);

    // Borders
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);

    return style;
  }

  /**
   * Creates the data cell style for the Excel sheet.
   */
  private CellStyle createDataStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();

    // Font
    Font font = workbook.createFont();
    font.setFontHeightInPoints((short) 10);
    style.setFont(font);

    // Alignment
    style.setAlignment(HorizontalAlignment.LEFT);

    // Borders
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);

    return style;
  }

  /**
   * A single exportable column: the DTO property path used to look up its value, and the
   * translation key used to render its header.
   */
  public record ExportColumn(String columnName, String translationKey) {

  }


  /**
   * Creates a cell with the given value and style.
   */
  protected void createCell(Row row, int column, String value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value != null ? value : "-");
    cell.setCellStyle(style);
  }

  protected String formatActiveStatus(boolean active, String locale) {
    return translationReaderService.translate(active ? ACTIVE_STATUS_KEY : INACTIVE_STATUS_KEY,
        locale);
  }

  /**
   * Formats a LocalDateTime to the display format.
   */
  protected String formatDateTime(LocalDateTime dateTime) {
    if (dateTime == null) {
      return "-";
    }
    return dateTime.format(dateTimeFormatter);
  }

  /**
   * Formats a LocalDate to the display format.
   */
  protected String formatDate(LocalDate date) {
    if (date == null) {
      return "-";
    }
    return date.format(dateFormatter);
  }

  /**
   * Removes columns an exporter doesn't want in its default set (e.g. audit metadata) from
   * {@code defaultColumns}. Static because subclasses call it before {@code super(...)}.
   */
  protected static void excludeFromDefaultColumns(List<String> defaultColumns,
      Set<String> nonDefaultColumns) {
    defaultColumns.removeIf(nonDefaultColumns::contains);
  }

  /**
   * Adds/overrides translations for fields inherited from a superclass DTO. No-op by default;
   * override in a subclass when custom labels are needed.
   */
  protected void overrideInheritedFieldTranslations(Map<String, String> headers) {
  }
}
