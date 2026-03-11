package gr.uniwa.unihealth.backend.controller;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.controller.request.SetGroupStatusCommand;
import gr.uniwa.unihealth.backend.controller.util.ControllerUtils;
import gr.uniwa.unihealth.backend.dto.GroupDTO;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.service.GroupReaderService;
import gr.uniwa.unihealth.backend.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("group")
@RequiredArgsConstructor
public class GroupController {

  private final GroupReaderService readerService;
  private final GroupService service;
  private final ControllerUtils controllerUtils;

  @PostMapping
  @Operation(summary = "Creates a new group",
      description = "Creates a new group in the application database.")
  public String create(@RequestBody @Valid GroupDTO dto) {
    return service.create(dto);
  }

  @Operation(summary = "Updates an existing group",
      description = "Updates an existing group in the application database.")
  @PutMapping("{id}")
  public void update(@PathVariable String id, @RequestBody @Valid GroupDTO dto) {
    service.update(id, dto);
  }

  @Operation(summary = "Deletes an existing group",
      description = "Deletes an existing group from the application database.")
  @DeleteMapping("{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }

  @Operation(summary = "Finds a single group by id",
      description = "Returns the details of a single group.")
  @GetMapping("{id}")
  public GroupDTO findById(@PathVariable String id) {
    return readerService.findById(id);
  }

  @Operation(summary = "Finds all Groups.",
      description = "Returns the data of the available Groups along with pagination information.")
  @PostMapping("_page")
  public Page<GroupDTO> findPage(@RequestBody(required = false) Map<String, Object> requestBody) {
    Map.Entry<Predicate, Pageable> predicateAndPageable =
        controllerUtils.getPredicateAndPageable(requestBody, User.class);
    return readerService.findAll(predicateAndPageable.getKey(), predicateAndPageable.getValue());
  }

  @Operation(summary = "Change the status of a Group.",
      description = "Changes the status of a Group to disabled or enabled.")
  @PutMapping("_set_group_status")
  public boolean setGroupStatus(@RequestBody SetGroupStatusCommand setGroupStatusCommand) {
    return service.setGroupStatus(setGroupStatusCommand.id(), setGroupStatusCommand.active());
  }


}
