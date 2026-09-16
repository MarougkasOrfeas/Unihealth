package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.SymptomFactor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SymptomFactorRepository extends BaseRepository<SymptomFactor> {

  List<SymptomFactor> findBySymptomItemSlugOrderByDisplayOrderAsc(String slug);
}
