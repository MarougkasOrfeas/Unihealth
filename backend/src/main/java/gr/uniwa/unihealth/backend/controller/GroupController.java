package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.controller.request.SetActiveStatusCommand;
import gr.uniwa.unihealth.backend.dto.GroupDTO;
import gr.uniwa.unihealth.backend.model.enums.Permission;
import gr.uniwa.unihealth.backend.service.BaseReaderService;
import gr.uniwa.unihealth.backend.service.BaseUpdatableService;
import gr.uniwa.unihealth.backend.service.GroupReaderService;
import gr.uniwa.unihealth.backend.service.GroupService;
import gr.uniwa.unihealth.backend.service.export.ExportService;
import gr.uniwa.unihealth.backend.service.export.GroupExportService;
import gr.uniwa.unihealth.backend.service.permission.UserPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("group")
@RequiredArgsConstructor
public class GroupController extends BaseUpdateableController<GroupDTO> {

  private final GroupReaderService readerService;
  private final GroupService service;
  private final GroupExportService exportService;
  private final UserPermissionService userPermissionService;

  @Operation(summary = "Change the status of a Group.",
      description = "Changes the status of a Group to disabled or enabled.")
  @PutMapping("_set_group_status")
  public boolean setGroupStatus(@RequestBody SetActiveStatusCommand setActiveStatusCommand) {
    userPermissionService.userHasGlobalPermissionOrThrow(Permission.ADMIN);
    return service.setGroupStatus(setActiveStatusCommand.id(), setActiveStatusCommand.active());
  }

  @Operation(summary = "Finds all active Groups.",
      description = "Returns the data of all active Groups.")
  @GetMapping("_active")
  public List<GroupDTO> findAllActive() {
    userPermissionService.userHasGlobalPermissionOrThrow(Permission.ADMIN);
    return readerService.findAllActive();
  }

  @Override
  protected BaseUpdatableService<GroupDTO> getService() {
    return service;
  }

  @Override
  protected BaseReaderService<GroupDTO> getReaderService() {
    return readerService;
  }

  @Override
  protected ExportService<GroupDTO> getExportService() {
    return exportService;
  }


  @Override
  protected Collection<Permission> getReadPermissions() {
    return List.of(Permission.ADMIN);
  }
}
