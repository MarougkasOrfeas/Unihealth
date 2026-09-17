package gr.uniwa.unihealth.backend.service.impl;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import gr.uniwa.unihealth.backend.dto.GroupDTO;
import gr.uniwa.unihealth.backend.mapper.UniGroupMapper;
import gr.uniwa.unihealth.backend.model.Department;
import gr.uniwa.unihealth.backend.model.QUniGroup;
import gr.uniwa.unihealth.backend.model.UniGroup;
import gr.uniwa.unihealth.backend.repository.DepartmentRepository;
import gr.uniwa.unihealth.backend.repository.UniGroupRepository;
import gr.uniwa.unihealth.backend.service.BaseReaderWithSearchService;
import gr.uniwa.unihealth.backend.service.GroupReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation for {@link GroupReaderService}.
 *
 * @author omaro
 */
@Service
@RequiredArgsConstructor
public class GroupReaderServiceImpl extends BaseReaderServiceImpl<GroupDTO, UniGroup>
    implements GroupReaderService, BaseReaderWithSearchService<GroupDTO> {

  private final UniGroupMapper mapper;
  private final UniGroupRepository repository;
  private final DepartmentRepository departmentRepository;

  @Override
  protected UniGroupMapper getMapper() {
    return mapper;
  }

  @Override
  protected UniGroupRepository getRepository() {
    return repository;
  }

  @Override
  public List<GroupDTO> findAllActive() {
    return repository.findByActiveTrue().stream().map(mapper::mapToDTO).toList();
  }

  /**
   * Free-text search over the columns the list screen actually shows. Without this,
   * {@link BaseReaderServiceImpl#createPredicateFromParams} drops the {@code search} parameter
   * entirely, because it only applies it to readers implementing
   * {@link BaseReaderWithSearchService}.
   */
  @Override
  public BooleanExpression buildSearchPredicate(BuildSearchPredicateParams params) {
    String search = params.search();
    if (search == null || search.isBlank()) {
      return null;
    }

    QUniGroup group = QUniGroup.uniGroup;
    return group.name.containsIgnoreCase(search)
        .or(group.description.containsIgnoreCase(search));
  }

  @Override
  public Page<GroupDTO> findAll(Predicate predicate, Pageable pageable) {
    Page<GroupDTO> page = super.findAll(predicate, pageable);
    addDepartments(page.getContent());
    return page;
  }

  private void addDepartments(List<GroupDTO> groups) {
    if (groups == null || groups.isEmpty()) {
      return;
    }

    List<String> groupIds = groups.stream().map(GroupDTO::getId).toList();

    var namesByGroupId = departmentRepository.findByGroupIdIn(groupIds).stream()
        .filter(department -> department.getGroup() != null).collect(
            java.util.stream.Collectors.groupingBy(department -> department.getGroup().getId(),
                java.util.stream.Collectors.mapping(Department::getName,
                    java.util.stream.Collectors.toList())));

    groups.forEach(
        group -> group.setDepartments(namesByGroupId.getOrDefault(group.getId(), List.of())));
  }

}
