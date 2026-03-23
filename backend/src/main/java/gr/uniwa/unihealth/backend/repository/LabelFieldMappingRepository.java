package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.LabelFieldMapping;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabelFieldMappingRepository extends BaseRepository<LabelFieldMapping> {

  List<LabelFieldMapping> findByActiveTrueAndCalculatedFieldFalseAndMixedRuleFalse();

  List<LabelFieldMapping> findByActiveTrueAndCalculatedFieldTrueAndMixedRuleFalse();

  List<LabelFieldMapping> findByLabelCodeInAndActiveTrue(List<String> labelCodes);


  // ── Used for admin/debugging: fetch all rules in a group ─────────────────
  List<LabelFieldMapping> findByLabelGroupAndActiveTrue(String labelGroup);

  // ── Used for admin: fetch a specific rule by its code ────────────────────
  Optional<LabelFieldMapping> findByLabelCode(String labelCode);

}
