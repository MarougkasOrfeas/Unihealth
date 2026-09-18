package gr.uniwa.unihealth.backend.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum UploadValidationPolicy {
  ATTACHMENT(Set.of("application/pdf", "image/jpeg", "image/png", "image/tiff")),

  IMAGE(Set.of("image/jpeg", "image/png", "image/tiff"));

  private final Set<String> allowedContentTypes;
}
