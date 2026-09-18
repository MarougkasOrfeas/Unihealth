package gr.uniwa.unihealth.backend.dto;

import gr.uniwa.unihealth.backend.model.enums.MedicalTestCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class FileDTO extends BaseUpdatableDTO {

  @Size(max = 255)
  private String name;

  @Size(max = 250)
  private String description;

  private boolean active;

  private String contentType;

  private long fileSize;

  /**
   * The owner, stamped from the authenticated principal when the file is created.
   *
   * <p>Never trusted from the request. Anything a client sends here is overwritten, and the mapper
   * ignores it on update.
   */
  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  private String userId;

  /**
   * When the examination took place.
   *
   * <p>{@code @DateTimeFormat} is required rather than decorative: this arrives as a multipart form
   * field, which is bound from a raw string and does not pass through the JSON deserialiser.
   */
  @PastOrPresent
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate examDate;

  private MedicalTestCategory category;

  /**
   * Deliberately not {@code @NotNull}: the same DTO is the body of {@code PUT {id}}, where it
   * arrives as JSON and can never carry a file. The empty check belongs to — and already lives in —
   * the MIME validator, which runs on the upload path only.
   */
  private MultipartFile multipartFile;
}
