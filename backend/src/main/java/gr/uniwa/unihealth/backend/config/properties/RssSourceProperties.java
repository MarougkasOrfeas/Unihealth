package gr.uniwa.unihealth.backend.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.rss")
public class RssSourceProperties {

  private List<Source> sources = new ArrayList<>();


  @Getter
  @Setter
  public static class Source {
    private String name;
    private String url;
  }
}
