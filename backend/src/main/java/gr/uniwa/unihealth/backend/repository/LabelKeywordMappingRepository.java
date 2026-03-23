package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.LabelKeywordMapping;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelKeywordMappingRepository extends BaseRepository<LabelKeywordMapping> {

  List<LabelKeywordMapping> findByKeywordTypeAndActiveTrue(String allergy);
}
