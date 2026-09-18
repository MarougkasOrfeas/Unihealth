package gr.uniwa.unihealth.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One batched measurement submitted by the client: engagement with a single label since the last
 * flush. Values are deltas to add, not totals — the client never sees the stored total.
 */
@Getter
@Setter
@NoArgsConstructor
public class LabelUsageDTO {

  @NotBlank
  @Size(max = 64)
  private String labelCode;

  @PositiveOrZero
  private long viewSeconds;

  @PositiveOrZero
  private int interactionCount;

  public LabelUsageDTO(String labelCode, long viewSeconds, int interactionCount) {
    this.labelCode = labelCode;
    this.viewSeconds = viewSeconds;
    this.interactionCount = interactionCount;
  }
}
