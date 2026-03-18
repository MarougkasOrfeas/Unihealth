package gr.uniwa.unihealth.backend.service.impl;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.dto.DepartmentDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.DepartmentMapper;
import gr.uniwa.unihealth.backend.model.Department;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.DepartmentRepository;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.DepartmentReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentReaderServiceImpl extends BaseReaderServiceImpl<DepartmentDTO, Department>
    implements DepartmentReaderService {

  private final DepartmentMapper mapper;
  private final DepartmentRepository repository;
  private final UserRepository userRepository;

  @Override
  protected BaseEntityMapper<DepartmentDTO, Department> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<Department> getRepository() {
    return repository;
  }

  @Override
  public List<DepartmentDTO> findAllActive() {
    return repository.findByActiveTrue().stream().map(mapper::mapToDTO).toList();
  }

  @Override
  public Page<DepartmentDTO> findAll(Predicate predicate, Pageable pageable) {
    Page<DepartmentDTO> page = super.findAll(predicate, pageable);
    addUsers(page.getContent());
    return page;
  }

  @Override
  public List<DepartmentDTO> findAllActiveByGroupName(String groupName) {
    return repository.findByActiveTrueAndGroupActiveTrueAndGroupNameOrderByNameAsc(groupName)
        .stream()
        .map(mapper::mapToDTO)
        .toList();
  }

  private void addUsers(List<DepartmentDTO> departments) {
    if (departments == null || departments.isEmpty()) {
      return;
    }

    List<String> departmentIds = departments.stream().map(DepartmentDTO::getId).toList();

    var usernamesByDepartmentId = userRepository.findByDepartmentIdIn(departmentIds).stream()
        .filter(user -> user.getDepartment() != null).collect(
            java.util.stream.Collectors.groupingBy(user -> user.getDepartment().getId(),
                java.util.stream.Collectors.mapping(User::getUsername,
                    java.util.stream.Collectors.toList())));

    departments.forEach(department -> department.setUsers(
        usernamesByDepartmentId.getOrDefault(department.getId(), List.of())));
  }
}
