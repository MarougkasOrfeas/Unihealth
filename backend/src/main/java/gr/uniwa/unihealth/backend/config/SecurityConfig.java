package gr.uniwa.unihealth.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application.
 *
 * @author omaro
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwkSetUri;

  @Value("${unihealth.security.jwt.expected-issuer}")
  private String expectedIssuer;

  @Value("${springdoc.api-docs.enabled}")
  private boolean apiDocsEnabled;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()).sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> {
          if (apiDocsEnabled) {
            auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                "/api-docs/**").permitAll();
          }

          auth.anyRequest().authenticated();
        }).oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(expectedIssuer);

    NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    jwtDecoder.setJwtValidator(validator);

    return jwtDecoder;
  }
}
