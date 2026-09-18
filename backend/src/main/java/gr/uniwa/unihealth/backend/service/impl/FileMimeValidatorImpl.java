package gr.uniwa.unihealth.backend.service.impl;


import gr.uniwa.unihealth.backend.exception.UNIHEALTHException;
import gr.uniwa.unihealth.backend.model.enums.UploadValidationPolicy;
import gr.uniwa.unihealth.backend.service.FileMimeValidator;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class FileMimeValidatorImpl implements FileMimeValidator {

  private static final Detector DETECTOR = new DefaultDetector();

  @Override
  public String validate(MultipartFile file, UploadValidationPolicy policy) {
    if (file == null || file.isEmpty()) {
      throw new UNIHEALTHException("file_empty", "File empty", null);
    }

    Set<String> allowedMimeTypes = policy.getAllowedContentTypes();
    try (InputStream inputStream = TikaInputStream.get(file.getInputStream())) {
      String detectedMimeType =
          DETECTOR.detect(inputStream, new Metadata()).getBaseType().toString();

      if (!allowedMimeTypes.contains(detectedMimeType)) {
        throw new UNIHEALTHException("unsupported_file_type",
            "Unsupported file type: " + detectedMimeType, null);
      }
      return detectedMimeType;
    } catch (IOException e) {
      throw new UNIHEALTHException("file_read_error", "Error reading file", e);
    }
  }
}
