package gr.uniwa.unihealth.backend.service.export.impl;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.controller.response.FieldDetail;
import gr.uniwa.unihealth.backend.dto.GroupDTO;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.service.GroupReaderService;
import gr.uniwa.unihealth.backend.service.TranslationReaderService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.export.GroupExportService;
import gr.uniwa.unihealth.backend.service.impl.ExportServiceImpl;
import gr.uniwa.unihealth.backend.service.permission.UserPermissionService;
import gr.uniwa.unihealth.backend.service.util.DateUtils;
import gr.uniwa.unihealth.backend.service.util.ServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupExportServiceImpl extends ExportServiceImpl<GroupDTO>
    implements GroupExportService {

  @Autowired
  protected GroupExportServiceImpl(TenantContext tenantContext,
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
    super(tenantContext, translationReaderService, headers, defaultColumns,
        DateUtils.DATE_TIME_FORMATTER, DateUtils.EXPORT_DATE_FORMATTER, groupReaderService,
        userPermissionService);
  }

  @Override
  protected void createDataRow(Sheet sheet, int rowNum, GroupDTO dto, CellStyle dataStyle,
      String locale) {

  }

  @Override
  protected String getExportFilename(String locale) {
    String tenantName = tenantContext.getCurrentTenant();
    String timestamp = LocalDateTime.now().format(DateUtils.TIMESTAMP_FORMATTER);

    return String.format("%s_%s_%s.xlsx",
        translationReaderService.translate("group.management.title", locale), tenantName,
        timestamp);
  }

  @Override
  protected String getSheetName(String locale) {
    return translationReaderService.translate("group.management.title", locale);
  }
}
