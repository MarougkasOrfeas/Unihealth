package gr.uniwa.unihealth.backend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
  String serverUrl,
  String realmMaster,
  String realmUnihealth,
  String clientId,
  String username,
  String password) {
}
