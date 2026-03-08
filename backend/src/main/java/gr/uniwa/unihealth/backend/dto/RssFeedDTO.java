package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RssFeedDTO extends BaseUpdatableDTO {

  private String title;
  private String summary;
  private String link;
  private String imageUrl;
  private LocalDateTime publishedDate;
  private String sourceName;
}
