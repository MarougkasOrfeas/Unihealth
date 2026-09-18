package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.ReferenceDataVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Plain {@link JpaRepository} rather than the project's {@code BaseRepository}: this table has a
 * natural string key and none of the audit, facet or QueryDSL machinery the base repository exists
 * to provide.
 */
@Repository
public interface ReferenceDataVersionRepository extends JpaRepository<ReferenceDataVersion, String> {
}
