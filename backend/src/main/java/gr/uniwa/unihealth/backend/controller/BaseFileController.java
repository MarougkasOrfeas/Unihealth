package gr.uniwa.unihealth.backend.controller;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.FileDTO;
import gr.uniwa.unihealth.backend.model.enums.UploadValidationPolicy;
import gr.uniwa.unihealth.backend.service.*;
import gr.uniwa.unihealth.backend.service.export.ExportService;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Collection;
import java.util.List;

/**
 * Upload, download and delete for a file-owning feature.
 *
 * <p>Deliberately owns no notion of who a file belongs to. A subclass decides that, and must scope
 * every inherited read — see {@code MedicalTestFileController} for what that involves and why it is
 * not optional.
 */
@Slf4j
public abstract class BaseFileController extends BaseUpdateableController<FileDTO> {

  /** Matches the {@code name} column width. */
  private static final int MAX_NAME_LENGTH = 255;

  // Protected rather than private: a subclass has to reach these to enforce ownership before
  // delegating, and there is no other handle on them.
  @Autowired
  protected FileService service;

  @Autowired
  protected FileReaderService readerService;

  @Autowired
  protected FileMimeValidator fileMimeValidator;

  @Autowired
  protected VirusScanService virusScanService;

  /**
   * {@code consumes} is declared so the generated API description says multipart rather than
   * advertising every DTO field as a query parameter, which is what {@code @ModelAttribute} implies
   * on its own.
   */
  @Operation(summary = "Uploads a file")
  @PostMapping(value = "_upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UploadResponse upload(@ModelAttribute @Valid FileDTO dto) {

    String fileType =
        fileMimeValidator.validate(dto.getMultipartFile(), getUploadValidationPolicy());
    dto.setContentType(fileType);
    dto.setFileSize(dto.getMultipartFile().getSize());
    dto.setActive(true);
    dto.setName(resolveName(dto));

    virusScanService.scanForViruses(() -> dto.getMultipartFile().getInputStream());

    String fileId = getFileUploadingService().createFile(dto);

    return new UploadResponse(fileId, dto.getName(), dto.getContentType(), dto.getFileSize());
  }

  /**
   * Settles on the name to store.
   *
   * <p>Two things go wrong if this is left as a plain assignment from the original file name.
   * {@code @Valid} ran at binding time, so a name taken from the upload afterwards was never
   * length-checked and can overflow the column — turning a long file name into a 500 rather than a
   * 400. And multipart form fields never pass through the NFC normalisation applied to JSON bodies,
   * so a Greek name uploaded from macOS arrives decomposed and never compares equal to the same
   * name typed into the search box.
   *
   * <p>{@code FilenameUtils.getName} drops any directory part, which covers both the
   * {@code C:\fakepath\} some browsers send and anything deliberate.
   */
  private String resolveName(FileDTO dto) {
    String name = StringUtils.isNotBlank(dto.getName()) ?
        dto.getName() :
        FilenameUtils.getName(dto.getMultipartFile().getOriginalFilename());

    name = Normalizer.normalize(StringUtils.trimToEmpty(name), Normalizer.Form.NFC);

    if (StringUtils.isBlank(name)) {
      throw ExceptionUtils.createException(QCouldNotSaveException.class, "global.file.name.empty",
          "The uploaded file has no usable name");
    }
    // Rejected rather than truncated: silently losing the extension is worse than telling the
    // student to shorten the name.
    if (name.length() > MAX_NAME_LENGTH) {
      throw ExceptionUtils.createException(QCouldNotSaveException.class, "global.file.name.too.long",
          "File name longer than {} characters", MAX_NAME_LENGTH);
    }

    return name;
  }

  @Operation(summary = "Deletes an existing resource",
      description = "Deletes an existing resource from the application database.")
  @DeleteMapping("_delete_file")
  public void deleteFile(@RequestParam String fileId) {
    getFileUploadingService().deleteFile(fileId);
  }

  @Operation(summary = "Downloads a file")
  @GetMapping("{id}/_download")
  public ResponseEntity<InputStreamResource> download(@PathVariable String id) {

    FileDTO fileDTO = getReaderService().findById(id);

    MediaType mediaType = StringUtils.isNotBlank(fileDTO.getContentType()) ?
        MediaType.parseMediaType(fileDTO.getContentType()) :
        MediaType.APPLICATION_OCTET_STREAM;

    InputStream fileInputStream = service.getFile(id);
    return ResponseEntity.ok().contentType(mediaType).contentLength(fileDTO.getFileSize())
        .header(HttpHeaders.CONTENT_DISPOSITION,
            // The charset overload matters here and is not cosmetic. Without it the name is written
            // as ISO-8859-1, which mangles or drops every Greek file name — in this application,
            // very nearly all of them.
            ContentDisposition.attachment().filename(fileDTO.getName(), StandardCharsets.UTF_8)
                .build().toString())
        .body(new InputStreamResource(fileInputStream));
  }

  @Operation(summary = "Checks whether the caller already has a file with this name")
  @GetMapping(value = "_is_name_available")
  public void isNameAvailable(@RequestParam String name) {
    getFileUploadingService().validateFileNameAvailable(name);
  }

  /** Files are created by uploading them; the generic create routes would bypass every check. */
  @Override
  public Collection<String> createMultiple(List<FileDTO> dtos) {
    throw new QDoesNotExistException();
  }

  /**
   * Disabled along with the single delete it backs, because deleting by bare id says nothing about
   * who is asking. A subclass that wants deletion re-implements {@code delete(String)} on top of an
   * owner-scoped lookup.
   */
  @Override
  public void deleteMultiple(List<String> ids) {
    throw new QDoesNotExistException();
  }

  @Override
  protected BaseUpdatableService<FileDTO> getService() {
    return service;
  }

  @Override
  protected BaseReaderService<FileDTO> getReaderService() {
    return readerService;
  }

  @Override
  protected ExportService<FileDTO> getExportService() {
    return null;
  }

  protected abstract BaseFileUploadingService getFileUploadingService();

  protected abstract UploadValidationPolicy getUploadValidationPolicy();

  public record UploadResponse(String fileId, String filename, String contentType, long fileSize) {
  }
}
