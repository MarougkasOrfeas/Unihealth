package gr.uniwa.unihealth.backend.controller;

import com.eurodyn.qlack.fuse.lexicon.dto.LanguageDTO;
import gr.uniwa.unihealth.backend.service.TranslationReaderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for translations and languages.
 *
 * @author omaro
 */
@RestController
@RequestMapping("translation")
@RequiredArgsConstructor
public class TranslationController {

  private final TranslationReaderService translationReaderService;

  @Operation(summary = "Gets all languages of the system.",
      description = "Gets all languages of the system.")
  @GetMapping("languages")
  public List<LanguageDTO> getLanguages() {
    return translationReaderService.getLanguages();
  }

  @Operation(summary = "Get translations for given locale.",
      description = "Retrieves translations for given locale.")
  @GetMapping("{locale}")
  public Map<String, String> getTranslations(@PathVariable String locale) {
    return translationReaderService.getTranslations(locale);
  }
}
