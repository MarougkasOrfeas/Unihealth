package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.dto.FileDTO;

import java.io.InputStream;

public interface FileService extends BaseUpdatableService<FileDTO> {

  /**
   * Find the file using the id of record in FileEntity
   *
   * @param id of file.
   * @return InputStream of file.
   */
  InputStream getFile(String id);
}
