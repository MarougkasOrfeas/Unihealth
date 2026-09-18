package gr.uniwa.unihealth.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3ClientConfig {

  @Value("${file-system.host}")
  private String host;

  @Value("${s3-access.key}")
  private String accessKey;

  @Value("${s3-secret.key}")
  private String secretKey;

  @Value("${s3-region}")
  private String region;

  @Bean(destroyMethod = "close")
  public S3Client s3Client() {
    return S3Client.builder().endpointOverride(URI.create(host)).region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .forcePathStyle(true).build();
  }
}
