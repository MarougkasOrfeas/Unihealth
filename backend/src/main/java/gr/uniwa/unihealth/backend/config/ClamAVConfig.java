package gr.uniwa.unihealth.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.capybara.clamav.ClamavClient;

/**
 * ClamAV client config.
 *
 * @author omaro
 */
@Slf4j
@Configuration
public class ClamAVConfig {

  @Value("${clamav-client.host}")
  private String host;

  @Value("${clamav-client.port}")
  private Integer port;

  @Bean
  public ClamavClient clamavClient() {
    return new ClamavClient(host, port);
  }
}
