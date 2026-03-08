package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_rss_feed")
public class RssFeed extends BaseUpdatableEntity {

  private String title;
  private String summary;
  private String link;
  @Column(name = "image_url")
  private String imageUrl;
  @Column(name = "published_date")
  private LocalDateTime publishedDate;
  @Column(name = "source_name")
  private String sourceName;
}
