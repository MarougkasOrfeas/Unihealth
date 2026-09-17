package gr.uniwa.unihealth.backend.config.context;

import gr.uniwa.unihealth.backend.config.cache.RequestCache;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.exception.UNIHEALTHException;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Tenant context for managing the current tenant in the application.
 *
 * @author omaro
 */
@Component
@RequiredArgsConstructor
public class TenantContext {

  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private final Semaphore semaphore = new Semaphore(50);

  private final ScopedValue<RequestCache> REQUEST_CACHE = ScopedValue.newInstance();
  private final Map<String, Properties> TENANT_PROPERTIES = new TreeMap<>();

  private final ApplicationContext applicationContext;

  @Value("${tenants.location}")
  private String tenantsLocation;

  /**
   * Get the current tenant.
   *
   * @return The current tenant id.
   */
  public String getCurrentTenant() {
    return getSingleTenantOr(() -> getRequestCache().getTenantId().trim().toUpperCase(Locale.ROOT));
  }

  /**
   * Get the request cache for the current tenant.
   *
   * @return The request cache for the current tenant.
   * @throws IllegalStateException if no tenant is set in the tenant context.
   */
  public RequestCache getRequestCache() {
    return REQUEST_CACHE.orElseThrow(
        () -> ExceptionUtils.createException(IllegalStateException.class, null,
            "No tenant set in the tenant context"));
  }

  /**
   * Run the given runnable as the specified tenant.
   *
   * @param tenant   The tenant id.
   * @param runnable The runnable to execute.
   * @return A CompletableFuture representing the execution.
   */
  public CompletableFuture<Void> runAs(String tenant, Runnable runnable) {
    return runAs(tenant, runnable, true);
  }

  /**
   * Run the given runnable as the specified tenant, optionally in a virtual thread.
   *
   * @param tenant          The tenant id.
   * @param runnable        The runnable to execute.
   * @param inVirtualThread Whether to run in a virtual thread.
   * @return A CompletableFuture representing the execution.
   */
  public CompletableFuture<Void> runAs(String tenant, Runnable runnable, boolean inVirtualThread) {
    if (inVirtualThread) {
      return CompletableFuture.runAsync(() -> {
        boolean acquired = false;
        try {
          semaphore.acquire();
          acquired = true;
          ScopedValue.where(REQUEST_CACHE, new RequestCache(tenant)).run(runnable);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new UNIHEALTHException(null, "Task interrupted", e);
        } finally {
          if (acquired) {
            semaphore.release();
          }
        }
      }, executor);
    } else {
      ScopedValue.where(REQUEST_CACHE, new RequestCache(tenant)).run(runnable);
      return CompletableFuture.completedFuture(null);
    }
  }

  /**
   * Run the given consumer for each tenant synchronously.
   *
   * see {@link #runForEachTenantAsync(Consumer, String)} for an async version.
   */
  public void runForEachTenant(Consumer<String> consumer, String jobName) {
    try {
      runForEachTenantAsync(consumer, jobName).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new UNIHEALTHException(null, "Error running task for each tenant", e);
    }
  }

  /**
   * Run the given consumer for each tenant async.
   *
   * @param consumer The consumer to execute for each tenant.
   * @param jobName  The name of the job for locking purposes.
   * @return A CompletableFuture representing the execution.
   */
  public CompletableFuture<Void> runForEachTenantAsync(Consumer<String> consumer, String jobName) {
    Map<String, Properties> tenantProperties = getAllTenantProperties();
    LockProvider lockProvider = applicationContext.getBean(LockProvider.class);
    TransactionTemplate transactionTemplate = applicationContext.getBean(TransactionTemplate.class);

    return CompletableFuture.allOf(
        tenantProperties.keySet().stream().map(tenantId -> runAs(tenantId, () -> {
          LockConfiguration lockConfig =
              new LockConfiguration(Instant.now(), jobName, Duration.ofHours(1),
                  Duration.ofSeconds(1));
          lockProvider.lock(lockConfig).ifPresent(lock -> {
            try {
              transactionTemplate.executeWithoutResult(status -> consumer.accept(tenantId));
            } finally {
              lock.unlock();
            }
          });
        })).toList().toArray(new CompletableFuture[tenantProperties.size()]));
  }

  /**
   * Get the single tenant if only one exists, otherwise return the result of the elseSupplier.
   *
   * @param elseSupplier The supplier to call if there are multiple tenants.
   * @return The single tenant or the result of the elseSupplier.
   */
  public String getSingleTenantOr(Supplier<String> elseSupplier) {
    return getAllTenantProperties().size() == 1 ?
        getAllTenantProperties().keySet().iterator().next() :
        elseSupplier.get();
  }

  /**
   * Get the admin username for the current tenant.
   *
   * @return The admin username for the current tenant.
   */
  public String getCurrentTenantAdminUsername() {
    return getAllTenantProperties().get(getCurrentTenant()).getProperty("admin-username");
  }

  /**
   * Get all tenant properties.
   *
   * @return A map of tenant ids to their properties.
   */
  public synchronized Map<String, Properties> getAllTenantProperties() {
    if (TENANT_PROPERTIES.isEmpty()) {
      try {
        ClassLoader classLoader = getClass().getClassLoader();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        Resource[] resources = resolver.getResources(tenantsLocation.endsWith("/") ?
            tenantsLocation + "*.properties" :
            tenantsLocation + "/*.properties");

        TENANT_PROPERTIES.putAll(List.of(resources).stream().collect(Collectors.toMap(
            resource -> Optional.ofNullable(resource.getFilename())
                .map(fName -> fName.trim().toUpperCase())
                .map(fName -> fName.substring(0, fName.lastIndexOf('.'))).orElseThrow(),
            resource -> {
              Properties properties = new Properties();
              try (InputStream inputStream = resource.getInputStream()) {
                properties.load(inputStream);
              } catch (IOException e) {
                throw new UNIHEALTHException(null,
                    "Could not load tenant properties from location: ", e);
              }

              return properties;
            })));
      } catch (Exception e) {
        throw new UNIHEALTHException(null,
            "Could not load tenant resources from location: " + tenantsLocation, e);
      }
    }

    return TENANT_PROPERTIES;
  }
}
