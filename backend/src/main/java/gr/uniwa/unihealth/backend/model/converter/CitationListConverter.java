package gr.uniwa.unihealth.backend.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uniwa.unihealth.backend.dto.ai.CitationDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Stores the citations of one answer in a single column, as JSON.
 *
 * <p>Follows {@link LabelListConverter}, which is how this codebase already packs a list into one
 * column, but serialises rather than comma-joining: a citation has eight fields and its URL may
 * itself contain a comma, so the separator trick that works for label codes would corrupt the data
 * on the first NHS link with a query string.
 *
 * <p><b>Two deliberate divergences from {@code LabelListConverter}</b>, which throws on null or
 * empty input. That is right for labels, where an empty list means a bug upstream. Here:
 *
 * <ul>
 *   <li>An ungrounded answer legitimately has no citations, so empty round-trips to {@code null}
 *       rather than throwing.</li>
 *   <li>Unreadable JSON returns an empty list rather than throwing. A conversation is something a
 *       student opens to reread; one message with a citation column this code cannot parse should
 *       cost that message its source chips, not make the whole conversation unopenable.</li>
 * </ul>
 *
 * @author omaro
 */
@Slf4j
@Converter
public class CitationListConverter implements AttributeConverter<List<CitationDTO>, String> {

  /**
   * Its own mapper rather than an injected one: a JPA converter is instantiated by the persistence
   * provider, not by Spring, so there is nothing to inject into. This application also runs
   * Jackson 3 for HTTP while this column is bound with Jackson 2, and keeping them apart here is
   * clearer than depending on which would win.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  private static final TypeReference<List<CitationDTO>> TYPE = new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(List<CitationDTO> citations) {
    if (citations == null || citations.isEmpty()) {
      return null;
    }

    try {
      return MAPPER.writeValueAsString(citations);
    } catch (Exception e) {
      // Losing the sources is bad; losing the answer is worse. The message still saves.
      log.warn("Could not serialise citations; storing none: {}", e.getMessage());
      return null;
    }
  }

  @Override
  public List<CitationDTO> convertToEntityAttribute(String json) {
    if (StringUtils.isBlank(json)) {
      return List.of();
    }

    try {
      return MAPPER.readValue(json, TYPE);
    } catch (Exception e) {
      log.warn("Could not read stored citations; returning none: {}", e.getMessage());
      return List.of();
    }
  }
}
