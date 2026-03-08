package gr.uniwa.unihealth.backend.config;

import com.eurodyn.qlack.fuse.lexicon.service.LexiconConfigService;
import com.eurodyn.qlack.fuse.mailing.monitor.MailQueueMonitor;
import com.eurodyn.qlack.fuse.mailing.service.MailService;
import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.repository.ExtendedJpaRepositoryImpl;
import jakarta.persistence.EntityManagerFactory;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Persistence configuration for the application.
 *
 * @author omaro
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EntityScan(basePackages = {"gr.uniwa.unihealth.backend", "com.eurodyn.qlack.fuse.lexicon.model",
    "com.eurodyn.qlack.fuse.mailing.model"})
@EnableJpaRepositories(
    basePackages = {"gr.uniwa.unihealth.backend", "com.eurodyn.qlack.fuse.lexicon.repository",
        "com.eurodyn.qlack.fuse.mailing.repository"},
    repositoryBaseClass = ExtendedJpaRepositoryImpl.class)
@ComponentScan(basePackages = {"com.eurodyn.qlack.common", "com.eurodyn.qlack.fuse.lexicon",
    "com.eurodyn.qlack.fuse.mailing"}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = LexiconConfigService.class),
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MailService.class),
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MailQueueMonitor.class)})
@RequiredArgsConstructor
public class PersistenceConfig {

  @Value("${datasource.driver-class-name}")
  private String driverClassName;

  @Value("${spring.liquibase.changeLog}")
  private String changelogLocation;

  @Value("${schema.validation}")
  private boolean schemaValidation;

  private final ApplicationContext applicationContext;
  private final TenantContext tenantContext;
  private final AuthenticationContext authenticationContext;
  private final JpaProperties jpaProperties;

  @Bean
  public DataSource dataSource() throws IOException, LiquibaseException {
    Map<String, DataSource> resolvedDataSources = new HashMap<>();

    for (Map.Entry<String, Properties> entry : tenantContext.getAllTenantProperties().entrySet()) {
      String tenantId = entry.getKey();
      Properties properties = entry.getValue();

      String url = properties.getProperty("url");
      String username = properties.getProperty("username");
      String password = properties.getProperty("password");

      log.info("Configuring datasource for tenant '{}' at '{}' with user '{}'", tenantId, url,
          username);

      DataSource dataSource =
          DataSourceBuilder.create().url(url).username(username).password(password)
              .driverClassName(driverClassName).build();

      resolvedDataSources.put(tenantId, dataSource);

      SpringLiquibase liquibase = new SpringLiquibase();
      liquibase.setDataSource(dataSource);
      liquibase.setChangeLog(changelogLocation);
      liquibase.setShouldRun(true);
      liquibase.setResourceLoader(applicationContext);
      liquibase.afterPropertiesSet();

      if (schemaValidation) {
        validateSchema(tenantId, dataSource);
      }
    }

    AbstractRoutingDataSource dataSource = new AbstractRoutingDataSource() {

      @Override
      protected Object determineCurrentLookupKey() {
        return tenantContext.getCurrentTenant();
      }
    };

    dataSource.setTargetDataSources(new HashMap<>(resolvedDataSources));
    dataSource.afterPropertiesSet();

    return dataSource;
  }

  @Bean
  public AuditorAware<String> auditorProvider() {
    return () -> Optional.of(authenticationContext.getCurrentUsername());
  }

  private void validateSchema(String tenantId, DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean validationEntityManagerFactory =
        new LocalContainerEntityManagerFactoryBean();
    validationEntityManagerFactory.setDataSource(dataSource);
    validationEntityManagerFactory.setPackagesToScan("gr.uniwa.unihealth.backend",
        "com.eurodyn.qlack.fuse.lexicon.model");
    validationEntityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

    Map<String, Object> validationJpaProperties = new HashMap<>();
    validationJpaProperties.putAll(jpaProperties.getProperties());
    validationJpaProperties.put("hibernate.hbm2ddl.auto", "validate");
    validationEntityManagerFactory.setJpaPropertyMap(validationJpaProperties);

    EntityManagerFactory entityManagerFactory = null;
    try {
      validationEntityManagerFactory.afterPropertiesSet();
      entityManagerFactory = validationEntityManagerFactory.getObject();
    } catch (Exception e) {
      throw new IllegalStateException("Schema validation failed for tenant '" + tenantId + "'.", e);
    } finally {
      if (entityManagerFactory != null) {
        entityManagerFactory.close();
      }
      validationEntityManagerFactory.destroy();
    }
  }
}
