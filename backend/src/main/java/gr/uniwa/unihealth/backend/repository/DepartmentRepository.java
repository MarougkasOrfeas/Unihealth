package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.Department;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends BaseRepository<Department> {

  /**
   * Finds all active departments.
   *
   * @return List of active departments.
   */
  List<Department> findByActiveTrue();

  /**
   * Finds a department by its name, ignoring case.
   *
   * @param name the name of the department to find.
   * @return an Optional containing the found department, or empty if no department with the given
   * name exists.
   */
  Optional<Department> findByNameIgnoreCase(String name);

  List<Department> findByGroupIdIn(List<String> groupIds);

  List<Department> findByActiveTrueAndGroupActiveTrueAndGroupNameOrderByNameAsc(String groupName);
}
