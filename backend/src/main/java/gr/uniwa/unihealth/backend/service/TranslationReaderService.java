package gr.uniwa.unihealth.backend.service;

import com.eurodyn.qlack.fuse.lexicon.dto.LanguageDTO;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing translations in the application.
 *
 * @author omaro
 */
public interface TranslationReaderService {

  /**
   * Retrieves translations for a given locale.
   *
   * @param locale the locale for which to retrieve translations
   * @return a map of translation keys and their corresponding translated values for the specified
   * locale
   */
  Map<String, String> getTranslations(String locale);

  /**
   * Gets all languages of the system.
   *
   * @return a list of LanguageDTO objects representing all languages available in the system.
   */
  List<LanguageDTO> getLanguages();

  String translate(String key, String locale);
}
