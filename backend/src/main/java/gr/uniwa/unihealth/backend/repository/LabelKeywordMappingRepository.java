package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.LabelKeywordMapping;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelKeywordMappingRepository extends BaseRepository<LabelKeywordMapping> {

  /**
   * Loads the whole dictionary in one go, to be folded into an in-memory index at startup.
   *
   * <p>This replaces a pair of per-type lookups that ran on <em>every</em> profile save, before any
   * check that the user had written anything at all — so a student who ticked no boxes still paid
   * for two queries. The dictionary is immutable between deployments, so reading it once per tenant
   * per boot is the right shape.
   */
  List<LabelKeywordMapping> findByActiveTrue();
}
