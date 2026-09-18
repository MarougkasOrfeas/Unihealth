package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.service.impl.VirusScanServiceImpl.InputStreamSupplier;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.InputStream;
import java.util.Collection;

public interface FileHandlerService {

  PutObjectResponse uploadFile(String fileId, String contentType, Long contentSize,
      InputStreamSupplier inputStreamSupplier);

  DeleteObjectResponse deleteFile(String fileId);

  /**
   * Removes several stored files at once, once the surrounding transaction has committed.
   *
   * <p>Deleting them one by one the way {@link #deleteFile(String)} does would leave the ones
   * already removed gone if a later one failed and rolled the records back, so the records would
   * point at nothing.
   *
   * @param fileIds The ids of the files to remove from storage.
   */
  void deleteFiles(Collection<String> fileIds);

  InputStream getFile(String fileId);
}
