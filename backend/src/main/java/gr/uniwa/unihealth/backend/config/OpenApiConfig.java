package gr.uniwa.unihealth.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * Open API configuration for the application.
 *
 * @author omaro
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "UNIHEALTH-Webportal-API"))
public class OpenApiConfig {
}
