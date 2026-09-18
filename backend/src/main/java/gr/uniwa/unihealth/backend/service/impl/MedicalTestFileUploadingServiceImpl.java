package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.FileDTO;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.model.FileEntity;
import gr.uniwa.unihealth.backend.repository.FileRepository;
import gr.uniwa.unihealth.backend.service.FileService;
import gr.uniwa.unihealth.backend.service.MedicalTestFileUploadingService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one place that decides whose file is whose.
 *
 * <p>Ownership is resolved from the authenticated principal on every call and is never accepted as
 * an argument, so there is no parameter a caller could set to reach another student's documents.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MedicalTestFileUploadingServiceImpl implements MedicalTestFileUploadingService {

  private final FileService fileService;
  private final FileRepository fileRepository;
  private final UserReaderService userReaderService;

  /**
   * The owner is stamped here and nowhere else.
   *
   * <p>The DTO arrives from a multipart form, so it may well carry a {@code userId} a client
   * invented. Overwriting it unconditionally is what makes it safe for the mapper to map the field
   * at all.
   */
  @Override
  public String createFile(FileDTO fileDTO) {
    fileDTO.setUserId(currentUserId());
    return fileService.create(fileDTO);
  }

  /**
   * Loads by (id, owner) rather than by id and then comparing.
   *
   * <p>A file belonging to someone else is not "forbidden", it is absent — which is both the safe
   * answer and one branch fewer for a future reader to forget.
   */
  @Override
  public void deleteFile(String fileId) {
    FileEntity entity = fileRepository.findByIdAndUserId(fileId, currentUserId())
        .orElseThrow(() -> new QDoesNotExistException("File does not exist"));

    fileService.delete(entity.getId());
  }

  /**
   * Advisory, and scoped to the caller.
   *
   * <p>It answers "have I already uploaded something called this" so an upload dialog can warn. It
   * is deliberately not a constraint: the stored object is keyed by the row's id, and the default
   * name is whatever the operating system called the file, so a clinic exporting {@code results.pdf}
   * every month must not be blocked from uploading the next one.
   */
  @Override
  public void validateFileNameAvailable(String fileName) {
    if (fileRepository.existsByUserIdAndNameIgnoreCase(currentUserId(), fileName)) {
      throw ExceptionUtils.createException(QCouldNotSaveException.class, "global.file.name.taken",
          "A file named {} already exists for this user", fileName);
    }
  }

  private String currentUserId() {
    return userReaderService.findLoggedInUser().getId();
  }
}
