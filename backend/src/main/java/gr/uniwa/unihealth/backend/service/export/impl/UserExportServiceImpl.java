package gr.uniwa.unihealth.backend.service.export.impl;

import gr.uniwa.unihealth.backend.config.cache.RequestCache;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.controller.response.FieldDetail;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.service.GroupReaderService;
import gr.uniwa.unihealth.backend.service.TranslationReaderService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.export.UserExportService;
import gr.uniwa.unihealth.backend.service.impl.ExportServiceImpl;
import gr.uniwa.unihealth.backend.service.permission.UserPermissionService;
import gr.uniwa.unihealth.backend.service.util.DateUtils;
import gr.uniwa.unihealth.backend.service.util.ServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static gr.uniwa.unihealth.backend.model.QUser.user;


@Service
@Slf4j
public class UserExportServiceImpl extends ExportServiceImpl<UserDTO> implements UserExportService {

  private static final String CHECK_MARK = "✓";

  private static final String LANGUAGE_KEY_PREFIX = "global.language.";

  private final GroupReaderService groupReaderService;

  private static final Set<String> NON_DEFAULT_EXPORT_COLUMNS =
      Set.of(ServiceUtils.fieldName(user.createdBy), ServiceUtils.fieldName(user.modifiedBy),
          ServiceUtils.fieldName(user.modifiedOn), ServiceUtils.fieldName(user.createdOn));

  @Autowired
  protected UserExportServiceImpl(TenantContext tenantContext,
      TranslationReaderService translationReaderService, UserReaderService userReaderService,
      GroupReaderService groupReaderService, UserPermissionService userPermissionService) {
    Map<String, String> headers = new LinkedHashMap<>();

    List<FieldDetail> annotatedFieldLabels =
        ServiceUtils.getAnnotatedFieldLabel(UserDTO.class).stream()
            .sorted(Comparator.comparingInt(FieldDetail::exportOrder)).toList();

    annotatedFieldLabels.forEach(entry -> {
      headers.put(entry.field(), entry.label());
    });

    List<String> defaultColumns =
        annotatedFieldLabels.stream().filter(FieldDetail::defaultField).map(FieldDetail::field)
            .collect(Collectors.toList());

    excludeFromDefaultColumns(defaultColumns, NON_DEFAULT_EXPORT_COLUMNS);
    super(tenantContext, translationReaderService, headers, defaultColumns,
        DateUtils.DATE_TIME_FORMATTER, DateUtils.EXPORT_DATE_FORMATTER, userReaderService,
        userPermissionService);
    this.groupReaderService = groupReaderService;
  }

  /**
   * Writes one data row per {@link UserDTO}, driven by the saved column config.
   */
  @Override
  protected void createDataRow(Sheet sheet, int rowNum, UserDTO dto, CellStyle dataStyle,
      String locale) {

    RequestCache requestCache = tenantContext.getRequestCache();

    Row row = sheet.createRow(rowNum);

    List<ExportColumn> columns = exportColumns.get();
    for (int i = 0; i < columns.size(); i++) {
      String columnName = columns.get(i).columnName();
      super.createCell(row, i, resolveCellValue(dto, columnName, locale, requestCache), dataStyle);
    }
  }

  /**
   * Resolves the display value for a single export column. Roles, groups, dates, the address name
   * and the locale-aware columns need dedicated handling; everything else is a plain
   * {@link UserDTO} property.
   */
  private String resolveCellValue(UserDTO dto, String columnName, String locale,
      RequestCache requestCache) {
    if (ServiceUtils.fieldName(user.status).equals(columnName)) {
      return formatStatus(dto, locale);
    }
    if (ServiceUtils.fieldName(user.lastLogin).equals(columnName)) {
      return formatDateTime(dto.getLastLogin());
    }
    if (ServiceUtils.fieldName(user.createdOn).equals(columnName)) {
      return formatDateTime(dto.getCreatedOn());
    }
    if (ServiceUtils.fieldName(user.modifiedOn).equals(columnName)) {
      return formatDateTime(dto.getModifiedOn());
    }
    if (ServiceUtils.fieldName(user.deactivateOn).equals(columnName)) {
      return formatDateTime(dto.getDeactivateOn());
    }
    if (ServiceUtils.fieldName(user.deactivateAfter).equals(columnName)) {
      return formatDate(dto.getDeactivateAfter());
    }
    if (ServiceUtils.fieldName(user.deactivatedDueToInactivity).equals(columnName)) {
      return dto.isDeactivatedDueToInactivity() ? CHECK_MARK : null;
    }
    if (ServiceUtils.fieldName(user.language).equals(columnName)) {
      return formatLanguage(dto.getLanguage(), locale);
    }

    Object value = ServiceUtils.findPropertyValueInPath(dto, columnName);
    return value != null ? value.toString() : null;
  }

  private String getGroupName(String groupId, RequestCache requestCache) {
    return requestCache.cachedDTO(groupId, groupReaderService::findById).getName();
  }

  private String formatStatus(UserDTO dto, String locale) {
    if (dto.getStatus() == null) {
      return null;
    }
    return switch (dto.getStatus()) {
      case ACTIVE -> translationReaderService.translate("user.status.active.label", locale);
      case UNVERIFIED -> translationReaderService.translate("user.status.unverified.label", locale);
      case DEACTIVATED -> translationReaderService.translate("user.status.inactive.label", locale);
    };
  }

  /**
   * The DTO carries the language id, so it is resolved to a locale first and then translated with
   * the same {@code global.language.<locale>} key the UI uses.
   */
  private String formatLanguage(String languageId, String locale) {
    return translationReaderService.getLanguages().stream()
        .filter(languageDTO -> languageDTO.getId().equals(languageId)).findFirst().map(
            languageDTO -> translationReaderService.translate(
                LANGUAGE_KEY_PREFIX + languageDTO.getLocale(), locale))
        .orElseThrow(() -> new IllegalArgumentException("Unknown language id: " + languageId));
  }

  @Override
  protected String getExportFilename(String locale) {
    String tenantName = tenantContext.getCurrentTenant();
    String timestamp = LocalDateTime.now().format(DateUtils.TIMESTAMP_FORMATTER);

    return String.format("%s_%s_%s.xlsx",
        translationReaderService.translate("user.management.title", locale), tenantName, timestamp);
  }

  @Override
  protected String getSheetName(String locale) {
    return translationReaderService.translate("user.management.title", locale);
  }

}
