package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class RssFeedDTO extends BaseUpdatableDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String title;
  private String summary;
  private String link;
  private String imageUrl;
  private LocalDateTime publishedDate;
  private String sourceName;
}
