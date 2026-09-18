package gr.uniwa.unihealth.backend.controller;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.FacetDTO;
import gr.uniwa.unihealth.backend.dto.FileDTO;
import gr.uniwa.unihealth.backend.model.enums.Permission;
import gr.uniwa.unihealth.backend.model.enums.UploadValidationPolicy;
import gr.uniwa.unihealth.backend.repository.FileRepository;
import gr.uniwa.unihealth.backend.service.BaseFileUploadingService;
import gr.uniwa.unihealth.backend.service.MedicalTestFileUploadingService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("medical-test-file")
@RequiredArgsConstructor
public class MedicalTestFileController extends BaseFileController {

  private final MedicalTestFileUploadingService uploadingService;
  private final UserReaderService userReaderService;
  private final FileRepository fileRepository;

  @Override
  @Operation(summary = "Lists the caller's own medical test files")
  public Page<FileDTO> findPage(Map<String, Object> requestBody) {
    Map<String, Object> body = new HashMap<>();
    if (requestBody != null) {
      body.putAll(requestBody);
    }
    body.put("userId", currentUserId());

    return getListReaderService().findAll(body);
  }

  @Override
  @Operation(summary = "Facet options drawn only from the caller's own files")
  public List<String> loadFacetOptions(FacetDTO facetParams) {
    if (facetParams.getPredicateParams() == null) {
      facetParams.setPredicateParams(new HashMap<>());
    }
    facetParams.getPredicateParams().put("userId", currentUserId());

    return getListReaderService().loadFacetOptions(facetParams);
  }

  @Override
  public FileDTO findById(String id) {
    requireOwned(id);
    return super.findById(id);
  }

  @Override
  public Collection<FileDTO> findByIds(Collection<String> ids) {
    String userId = currentUserId();
    return super.findByIds(ids).stream().filter(dto -> userId.equals(dto.getUserId())).toList();
  }

  @Override
  public ResponseEntity<InputStreamResource> download(String id) {
    requireOwned(id);
    return super.download(id);
  }

  @Override
  public void update(String id, FileDTO dto) {
    requireOwned(id);
    super.update(id, dto);
  }

  @Override
  public void delete(String id) {
    uploadingService.deleteFile(id);
  }

  @Override
  protected BaseFileUploadingService getFileUploadingService() {
    return uploadingService;
  }

  @Override
  protected UploadValidationPolicy getUploadValidationPolicy() {
    return UploadValidationPolicy.ATTACHMENT;
  }

  @Override
  protected Collection<Permission> getReadPermissions() {
    return List.of();
  }

  private void requireOwned(String id) {
    if (!fileRepository.existsByIdAndUserId(id, currentUserId())) {
      throw new QDoesNotExistException("File does not exist");
    }
  }

  private String currentUserId() {
    return userReaderService.findLoggedInUser().getId();
  }
}
