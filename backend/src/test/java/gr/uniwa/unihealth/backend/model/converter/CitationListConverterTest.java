package gr.uniwa.unihealth.backend.model.converter;

import gr.uniwa.unihealth.backend.dto.ai.CitationDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The column a reopened conversation reads its sources from.
 *
 * <p>Worth testing on its own because the failure mode is silent: a citation that does not
 * round-trip does not throw, it simply produces a stored answer that claims to be grounded and
 * cannot say in what.
 *
 * @author omaro
 */
class CitationListConverterTest {

  private final CitationListConverter converter = new CitationListConverter();

  private static CitationDTO citation(String title, String url) {
    return new CitationDTO(title, "Overview", "NHS", url, false, "LOCAL_VETTED", "", "");
  }

  @Test
  @DisplayName("a citation survives the round trip with every field intact")
  void roundTrips() {
    List<CitationDTO> citations =
        List.of(citation("Headaches", "https://www.nhs.uk/conditions/headaches/"),
            new CitationDTO("Measles", "Symptoms", "ECDC", "https://www.ecdc.europa.eu/measles",
                true, "LIVE_WEB", "2026-09-01", "2026-09-21T10:00:00Z"));

    assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(citations)))
        .isEqualTo(citations);
  }

  @Test
  @DisplayName("a URL containing a comma survives, which is why this is JSON and not a joined list")
  void keepsCommasInsideUrls() {
    // The exact input that would have silently split under LabelListConverter's comma join.
    List<CitationDTO> citations =
        List.of(citation("Flu", "https://www.who.int/search?q=flu,influenza&lang=el"));

    assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(citations)))
        .singleElement()
        .extracting(CitationDTO::sourceUrl)
        .isEqualTo("https://www.who.int/search?q=flu,influenza&lang=el");
  }

  @Test
  @DisplayName("an ungrounded answer stores null rather than an empty array")
  void storesNothingForNoCitations() {
    // The deliberate divergence from LabelListConverter, which throws here. An answer from the
    // model's own training data legitimately has no sources; that is not a bug upstream.
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
  }

  @Test
  @DisplayName("an empty column reads back as an empty list, never null")
  void readsNothingAsEmpty() {
    // So callers never have to null-check a collection - the service maps straight over it.
    assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    assertThat(converter.convertToEntityAttribute("")).isEmpty();
    assertThat(converter.convertToEntityAttribute("   ")).isEmpty();
  }

  @Test
  @DisplayName("unreadable JSON costs that message its chips, not the whole conversation")
  void survivesCorruptJson() {
    // The other divergence. Throwing here would make one bad row unopenable for good, and a
    // conversation is something a student opens to reread.
    assertThat(converter.convertToEntityAttribute("{not json")).isEmpty();
  }
}
