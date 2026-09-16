package gr.uniwa.unihealth.backend.service.bootstrap;

import java.time.LocalDate;
import java.util.List;

/**
 * The committed on-disk NHS corpus read by the backend bootstrap process.
 *
 * <p>The crawler that creates this shape lives under {@code data/nhs-ingest}. The backend keeps its
 * own copy of the record so normal application startup does not depend on data-fetching tooling.
 *
 * @author omaro
 */
public record NhsSnapshot(Source source, List<Symptom> symptoms) {

  public record Source(String code, String name, String url, String licence, String licenceUrl,
                       String attributionText, String logoUrl, LocalDate retrievedOn) {

  }

  public record Symptom(String title, String slug, String synonyms, String sourceUrl,
                        String lastReviewed, String brief, String overviewText, String symptomsText,
                        String doText, String dontText, String seeDoctorIfText, String urgentText,
                        String emergencyText, String treatmentText, String causesText,
                        List<Cause> causes) {

  }

  public record Cause(String factorsText, String causeText, List<String> factors,
                      List<ConditionRef> conditions) {

  }

  public record ConditionRef(String name, String url) {

  }
}
