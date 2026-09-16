package gr.uniwa.unihealth.data.nhs;

import java.time.LocalDate;
import java.util.List;

/**
 * The on-disk form of an ingested corpus.
 *
 * <p>The crawler writes one of these to {@code resources/data/nhs/} and it is committed; every
 * subsequent application start seeds from the file rather than the network. That keeps startup
 * offline, makes the database reproducible for anyone marking the work, and means the crawl is an
 * explicit act rather than a side effect of running the app.
 *
 * @author omaro
 */
public record NhsSnapshot(Source source, List<Symptom> symptoms) {

  /** Publisher and licence, carried with the content so attribution cannot drift away from it. */
  public record Source(String code, String name, String url, String licence, String licenceUrl,
                       String attributionText, String logoUrl, LocalDate retrievedOn) {

  }

  public record Symptom(String title, String slug, String synonyms, String sourceUrl,
                        String lastReviewed, String brief, String overviewText, String symptomsText,
                        String doText, String dontText, String seeDoctorIfText, String urgentText,
                        String emergencyText, String treatmentText, String causesText,
                        List<Cause> causes) {

  }

  /** One row of the NHS symptoms-and-possible-causes table. */
  public record Cause(String factorsText, String causeText, List<String> factors,
                      List<ConditionRef> conditions) {

  }

  public record ConditionRef(String name, String url) {

  }
}
