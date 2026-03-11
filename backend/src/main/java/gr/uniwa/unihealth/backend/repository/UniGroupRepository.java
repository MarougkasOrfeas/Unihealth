package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.UniGroup;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniGroupRepository extends BaseRepository<UniGroup> {

  /**
   * Finds all active groups.
   *
   * @return List of active groups.
   */
  List<UniGroup> findByActiveTrue();

  /**
   * Finds a group by its name, ignoring case.
   *
   * @param name the name of the group to find.
   * @return an Optional containing the found group, or empty if no group with the given name
   * exists.
   */
  Optional<UniGroup> findByNameIgnoreCase(String name);
}
