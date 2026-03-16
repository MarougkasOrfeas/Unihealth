package gr.uniwa.unihealth.backend.config.cache;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component("rssCacheKeyHelper")
@RequiredArgsConstructor
public class RssCacheKeyHelper {

  private final TenantContext tenantContext;

  public String pageKey(Predicate predicate, Pageable pageable) {
    String tenantId = tenantContext.getCurrentTenant();
    String predicateKey = predicate != null ? predicate.toString() : "null";
    String pageNumber = pageable != null ? String.valueOf(pageable.getPageNumber()) : "0";
    String sort = pageable != null ? pageable.getSort().toString() : "UNSORTED";

    return tenantId + "|" + predicateKey + "|" + pageNumber + "|" + sort;
  }

  public String relevantFeedsKey(int limit) {
    return tenantContext.getCurrentTenant() + "|" + limit;
  }
}
