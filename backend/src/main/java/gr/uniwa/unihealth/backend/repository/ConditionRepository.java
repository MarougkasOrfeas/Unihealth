package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.Condition;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConditionRepository extends BaseRepository<Condition> {

  List<Condition> findByActiveTrueOrderByStartingLetterAscDisplayOrderAscNameAsc();

  Optional<Condition> findBySlugAndActiveTrue(String slug);

  Optional<Condition> findBySlug(String slug);

  @Query("""
      select c
      from Condition c
      where c.active = true
        and lower(c.name) like lower(concat('%', :search, '%'))
      order by c.startingLetter asc, c.displayOrder asc, c.name asc
      """)
  List<Condition> searchActive(@Param("search") String search);

  /**
   * The symptoms whose causes table points at this condition, which is what a condition page has
   * to show in place of a description - the condition pages themselves are never ingested.
   */
  @Query("""
      select distinct s.symptomItem.slug
      from SymptomCause s
        join s.conditions c
      where c.slug = :slug
        and s.symptomItem.active = true
      order by s.symptomItem.slug asc
      """)
  List<String> findSymptomSlugsForCondition(@Param("slug") String slug);
}
