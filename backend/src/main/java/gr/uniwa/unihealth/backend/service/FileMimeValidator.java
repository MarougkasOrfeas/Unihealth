package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.model.enums.UploadValidationPolicy;
import org.springframework.web.multipart.MultipartFile;

public interface FileMimeValidator {

  String validate(MultipartFile file, UploadValidationPolicy policy);
}
