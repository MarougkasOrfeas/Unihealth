package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.controller.request.SetActiveStatusCommand;
import gr.uniwa.unihealth.backend.dto.DepartmentDTO;
import gr.uniwa.unihealth.backend.model.enums.Permission;
import gr.uniwa.unihealth.backend.service.BaseReaderService;
import gr.uniwa.unihealth.backend.service.BaseUpdatableService;
import gr.uniwa.unihealth.backend.service.DepartmentReaderService;
import gr.uniwa.unihealth.backend.service.DepartmentService;
import gr.uniwa.unihealth.backend.service.export.ExportService;
import gr.uniwa.unihealth.backend.service.permission.UserPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("department")
@RequiredArgsConstructor
public class DepartmentController extends BaseUpdateableController<DepartmentDTO> {

  private final DepartmentReaderService readerService;
  private final DepartmentService service;
  private final UserPermissionService userPermissionService;

  @Operation(summary = "Change the status of a Department.",
      description = "Changes the status of a Department to disabled or enabled.")
  @PutMapping("_set_department_status")
  public boolean setDepartmentStatus(@RequestBody SetActiveStatusCommand setActiveStatusCommand) {
    userPermissionService.userHasGlobalPermissionOrThrow(Permission.ADMIN);
    return service.setDepartmentStatus(setActiveStatusCommand.id(),
        setActiveStatusCommand.active());
  }

  @Operation(summary = "Finds all active Departments.",
      description = "Returns the data of all active Departments.")
  @GetMapping("_active")
  public List<DepartmentDTO> findAllActive() {
    userPermissionService.userHasGlobalPermissionOrThrow(Permission.ADMIN);
    return readerService.findAllActive();
  }

  @Operation(summary = "Finds all active Departments by Group name.",
      description = "Returns the data of all active Departments that belong to the given active Group name.")
  @GetMapping("_active/by-group/{groupName}")
  public List<DepartmentDTO> findAllActiveByGroupName(@PathVariable String groupName) {
    userPermissionService.userHasGlobalPermissionOrThrow(Permission.ADMIN);
    return readerService.findAllActiveByGroupName(groupName);
  }


  @Override
  protected BaseUpdatableService<DepartmentDTO> getService() {
    return service;
  }

  @Override
  protected BaseReaderService<DepartmentDTO> getReaderService() {
    return readerService;
  }

  @Override
  protected Collection<Permission> getReadPermissions() {
    return List.of(Permission.ADMIN);
  }

  @Override
  protected ExportService<DepartmentDTO> getExportService() {
    return null;
  }
}
