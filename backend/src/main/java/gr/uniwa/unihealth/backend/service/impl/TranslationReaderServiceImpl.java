package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.fuse.lexicon.dto.LanguageDTO;
import com.eurodyn.qlack.fuse.lexicon.repository.ApplicationRepository;
import com.eurodyn.qlack.fuse.lexicon.service.GroupService;
import com.eurodyn.qlack.fuse.lexicon.service.KeyService;
import com.eurodyn.qlack.fuse.lexicon.service.LanguageService;
import com.eurodyn.qlack.fuse.lexicon.service.LexiconConfigService;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.service.TranslationReaderService;
import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Implementation for {@link TranslationReaderService}.
 *
 * @author omaro
 */
@Service
@Transactional
public class TranslationReaderServiceImpl extends LexiconConfigService
    implements TranslationReaderService {

  private static final List<String> localeOrder = Arrays.asList("el", "en");
  private static final String CONFIG_FILE_NAME = "qlack-lexicon-config.yaml";

  private final ApplicationRepository applicationRepository;
  private final LanguageService languageService;
  private final GroupService groupService;
  private final KeyService keyService;
  private final TenantContext tenantContext;

  /**
   * Public constructor.
   */
  public TranslationReaderServiceImpl(GroupService groupService, LanguageService languageService,
      KeyService keyService, ApplicationRepository applicationRepository,
      TenantContext tenantContext) {

    super(groupService, languageService, keyService, applicationRepository);
    this.applicationRepository = applicationRepository;
    this.languageService = languageService;
    this.groupService = groupService;
    this.keyService = keyService;
    this.tenantContext = tenantContext;
  }

  /**
   * Initialize the service for each tenant.
   */
  @Override
  @PostConstruct
  public void init() {
    tenantContext.runForEachTenant(tenantId -> {
      applicationRepository.findBySymbolicName(CONFIG_FILE_NAME).stream().findFirst()
          .ifPresent(application -> {
            ClassLoader classLoader = getClass().getClassLoader();
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
            Resource resource = resolver.getResource(CONFIG_FILE_NAME);

            try (InputStream inputStream = resource.getInputStream()) {
              String configurationFileChecksum = DigestUtils.md5Hex(inputStream);

              if (!application.getChecksum().equals(configurationFileChecksum)) {
                String groupId = groupService.findByTitle("ui_labels").getId();
                // TODO @Kostis This is temporary solution to clear existing keys.
                // Later when more languages are present I don't think this is a good idea.
                // Maybe if we later implement an import/export of keys this will not be necessary.
                keyService.deleteKeysByGroupId(groupId);
              }
            } catch (IOException e) {
              throw new RuntimeException("Failed to read translation configuration", e);
            }
          });

      super.init();
    }, "TranslationReaderServiceImpl.init");
  }

  @Override
  public Map<String, String> getTranslations(String locale) {
    return keyService.getTranslationsForLocale(locale);
  }

  @Override
  public List<LanguageDTO> getLanguages() {
    List<LanguageDTO> result = languageService.getLanguages(false);
    result.sort((a, b) -> {
      Integer aIndex = localeOrder.contains(a.getLocale()) ?
          localeOrder.indexOf(a.getLocale()) :
          Integer.MAX_VALUE;
      Integer bIndex = localeOrder.contains(b.getLocale()) ?
          localeOrder.indexOf(b.getLocale()) :
          Integer.MAX_VALUE;

      return aIndex.compareTo(bIndex);
    });
    return result;
  }
}
