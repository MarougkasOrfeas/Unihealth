package gr.uniwa.unihealth.backend.service;

/**
 * Owner-aware file handling for the medical test documents on «Οι Εξετάσεις Μου».
 *
 * <p>A named sub-interface rather than a direct implementation of {@link BaseFileUploadingService},
 * so that a second file-owning feature can be added later without the two competing to satisfy an
 * injection point by type.
 */
public interface MedicalTestFileUploadingService extends BaseFileUploadingService {
}
