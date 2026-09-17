package gr.uniwa.unihealth.backend.controller;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.dto.FacetDTO;
import gr.uniwa.unihealth.backend.exception.UNIHEALTHException;
import gr.uniwa.unihealth.backend.model.enums.Permission;
import gr.uniwa.unihealth.backend.service.BaseReaderService;
import gr.uniwa.unihealth.backend.service.BaseService;
import gr.uniwa.unihealth.backend.service.permission.UserPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class BaseController<D extends BaseDTO> {

  @Autowired
  protected UserPermissionService userPermissionService;

  @Operation(summary = "Creates a new resource",
      description = "Creates a new resource in the application database.")
  @PostMapping
  public String create(@RequestBody @Valid D dto) {
    return createMultiple(List.of(dto)).iterator().next();
  }

  @Operation(summary = "Creates multiple new resources",
      description = "Creates multiple new resources in the application database.")
  @PostMapping("_multiple")
  public Collection<String> createMultiple(@RequestBody @Valid List<D> dtos) {
    BaseService<D> service = getService();
    if (service == null) {
      throw new QDoesNotExistException();
    }

    return service.create(dtos);
  }

  @Operation(summary = "Deletes an existing resource",
      description = "Deletes an existing resource from the application database.")
  @DeleteMapping("{id}")
  public void delete(@PathVariable String id) {
    deleteMultiple(List.of(id));
  }

  @Operation(summary = "Deletes existing resources",
      description = "Deletes existing resources from the application database.")
  @DeleteMapping
  public void deleteMultiple(@RequestBody List<String> ids) {
    BaseService<D> service = getService();
    if (service == null) {
      throw new QDoesNotExistException();
    }

    service.delete(ids);
  }

  @Operation(summary = "Finds a single resource by id",
      description = "Returns the details of a single resource.")
  @GetMapping("{id}")
  public D findById(@PathVariable String id) {
    return getReaderService().findById(id);
  }

  /**
   * Batch-loads resources by id to increase performance for several associated master-data records.
   * (ex details page for Sorte.)
   */
  @Operation(summary = "Finds resources by ids",
      description = "Returns the resources matching the provided ids.")
  @PostMapping("_by_ids")
  public Collection<D> findByIds(@RequestBody Collection<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }

    return getReaderService().findByIds(ids).values();
  }

  @Operation(summary = "Finds resources",
      description = "Returns the data of the available resources along with pagination information.")
  @PostMapping("_page")
  public Page<D> findPage(@RequestBody(required = false) Map<String, Object> requestBody) {
    return getListReaderService().findAll(requestBody);
  }

  @Operation(summary = "Returns distinct facet options for a column")
  @PostMapping("_facet")
  public List<String> loadFacetOptions(@RequestBody FacetDTO facetParams) {
    return getListReaderService().loadFacetOptions(facetParams);
  }

  /**
   * The reader service the paged list and its facets are served from. Overridden by the controllers
   * whose list screen reads a projection rather than the entity itself.
   *
   * @return The reader service to list with.
   */
  protected BaseReaderService<D> getListReaderService() {
    return getReaderService();
  }

  @PostMapping("_available")
  public boolean available(@RequestBody D dto) {
    BaseService<D> service = getService();
    if (service == null) {
      throw new QDoesNotExistException();
    }

    try {
      service.validateAvailable(dto.getId(), dto);
      return true;
    } catch (UNIHEALTHException exception) {
      return false;
    }
  }

  protected abstract BaseService<D> getService();

  protected abstract BaseReaderService<D> getReaderService();

  protected abstract Collection<Permission> getReadPermissions();
}
