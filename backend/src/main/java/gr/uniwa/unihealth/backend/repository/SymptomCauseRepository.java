package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.SymptomCause;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SymptomCauseRepository extends BaseRepository<SymptomCause> {

  /**
   * Loads every cause row of a symptom with its factors and conditions attached. A symptom has at
   * most a couple of dozen rows, so fetching the lot and scoring in memory is cheaper and far
   * easier to follow than expressing the ranking in SQL.
   */
  @EntityGraph(attributePaths = {"factors", "conditions"})
  @Query("""
      select distinct c
      from SymptomCause c
      where c.symptomItem.slug = :slug
      order by c.displayOrder asc
      """)
  List<SymptomCause> findBySymptomSlugWithDetails(@Param("slug") String slug);
}
