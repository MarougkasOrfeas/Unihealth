package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.SymptomItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SymptomItemRepository extends BaseRepository<SymptomItem> {

  List<SymptomItem> findByActiveTrueOrderByStartingLetterAscDisplayOrderAscTitleAsc();

  Optional<SymptomItem> findBySlugAndActiveTrue(String slug);

  @Query("""
      select s
      from SymptomItem s
      where s.active = true
        and (
          lower(s.title) like lower(concat('%', :search, '%'))
          or lower(coalesce(s.brief, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(s.overviewText, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(s.symptomsText, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(s.doText, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(s.dontText, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(s.seeDoctorIfText, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(s.treatmentText, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(s.causesText, '')) like lower(concat('%', :search, '%'))
        )
      order by s.startingLetter asc, s.displayOrder asc, s.title asc
      """)
  List<SymptomItem> searchActive(@Param("search") String search);
}
