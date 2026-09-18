package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.FileDTO;

/**
 * The part of file handling that knows who a file belongs to.
 *
 * <p>No method takes an owner. That is the contract: an implementation resolves the owner from the
 * authenticated principal, so there is no parameter a caller could supply to reach somebody else's
 * file. An earlier draft threaded a {@code referenceId} through every method, which is the same
 * ownership decision made by whoever is holding the request.
 */
public interface BaseFileUploadingService {

  /** Stores the file and its metadata, stamping the caller as the owner. */
  String createFile(FileDTO fileDTO);

  /** Removes a file the caller owns. A file owned by anyone else does not exist. */
  void deleteFile(String fileId);

  /**
   * Throws when the caller already has a file by this name.
   *
   * <p>Advisory — it lets an upload dialog warn before submitting. It is not a constraint, and
   * nothing stops the same name being stored twice.
   */
  void validateFileNameAvailable(String fileName);
}
