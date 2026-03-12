package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.UnihealthAssistantItem;
import gr.uniwa.unihealth.backend.model.enums.AssistantSection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnihealthAssistantItemRepository extends BaseRepository<UnihealthAssistantItem> {

  List<UnihealthAssistantItem> findByActiveTrueAndSectionOrderByDisplayOrderAsc(
      AssistantSection section);

  List<UnihealthAssistantItem> findByActiveTrueOrderBySectionAscDisplayOrderAsc();

  @Query("""
          select u
          from UnihealthAssistantItem u
          where u.active = true
            and u.section = :section
            and (
              lower(u.title) like lower(concat('%', :search, '%'))
              or lower(u.brief) like lower(concat('%', :search, '%'))
              or lower(u.content) like lower(concat('%', :search, '%'))
            )
          order by u.displayOrder asc
      """)
  List<UnihealthAssistantItem> searchBySection(@Param("section") AssistantSection section,
      @Param("search") String search);
}
