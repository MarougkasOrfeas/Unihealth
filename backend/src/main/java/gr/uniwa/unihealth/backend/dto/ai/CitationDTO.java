package gr.uniwa.unihealth.backend.dto.ai;

/** * Represents a source used to support an AI answer.
 * @param title source title
 * @param sectionLabel relevant section of the source
 * @param sourceName source publisher
 * @param sourceUrl link to the source
 * @param urgent whether the source contains urgent guidance
 * @param sourceType source type, such as LOCAL_VETTED or LIVE_WEB
 * @param publishedAt when the source was published
 * @param retrievedAt when the source was retrieved
 *
 * * @author omaro */
public record CitationDTO(String title, String sectionLabel, String sourceName, String sourceUrl,
                          boolean urgent, String sourceType, String publishedAt,
                          String retrievedAt) {
}
