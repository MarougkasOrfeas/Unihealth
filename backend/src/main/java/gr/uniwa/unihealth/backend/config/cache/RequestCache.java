package gr.uniwa.unihealth.backend.config.cache;

import com.eurodyn.qlack.fuse.lexicon.dto.LanguageDTO;
import gr.uniwa.unihealth.backend.dto.BaseDTO;
import gr.uniwa.unihealth.backend.dto.RightsMatrix;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor
public class RequestCache implements Serializable {

  private final String tenantId;
  private final Map<String, Map<String, String>> translationsCache = new HashMap<>();
  private final Map<String, BaseDTO> dtoCache = new HashMap<>();
  private final Map<String, RightsMatrix> rightsMatrixCache = new HashMap<>();
  private List<LanguageDTO> languagesCache;

  /**
   * Cache a DTO for the current request.
   *
   * @param <T>             The type of the DTO.
   * @param id              The id of the DTO.
   * @param mappingFunction The function to map the id to the DTO.
   * @return The cached DTO.
   */
  @SuppressWarnings("unchecked")
  public <T extends BaseDTO> T cachedDTO(String id, Function<String, T> mappingFunction) {
    return (T) dtoCache.computeIfAbsent(id, i -> mappingFunction.apply(i));
  }

  /**
   * Cache a rights matrix for the current request.
   *
   * @param username        The username of the rights matrix.
   * @param mappingFunction The function to map the username to the rights matrix.
   * @return The cached rights matrix.
   */
  public RightsMatrix cachedRightsMatrix(String username,
      Function<String, RightsMatrix> mappingFunction) {
    return rightsMatrixCache.computeIfAbsent(username, u -> mappingFunction.apply(u));
  }

  /**
   * Cache the languages of the system for the current request.
   *
   * @param supplier The supplier fetching the languages.
   * @return The cached languages.
   */
  public List<LanguageDTO> cachedLanguages(Supplier<List<LanguageDTO>> supplier) {
    if (CollectionUtils.isEmpty(languagesCache)) {
      languagesCache = supplier.get();
    }
    return languagesCache;
  }
}
