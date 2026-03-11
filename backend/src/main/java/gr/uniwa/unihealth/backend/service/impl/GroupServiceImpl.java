package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QAlreadyExistsException;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.dto.GroupDTO;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.mapper.BaseUpdatableEntityMapper;
import gr.uniwa.unihealth.backend.mapper.UniGroupMapper;
import gr.uniwa.unihealth.backend.model.Department;
import gr.uniwa.unihealth.backend.model.UniGroup;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.DepartmentRepository;
import gr.uniwa.unihealth.backend.repository.UniGroupRepository;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.GroupService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementation for {@link GroupService}.
 *
 * @author European Dynamics SA
 */
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends BaseUpdatableServiceImpl<GroupDTO, UniGroup>
    implements GroupService {

  private final GroupReaderServiceImpl readerService;
  private final UniGroupMapper mapper;
  private final UniGroupRepository repository;
  private final DepartmentRepository departmentRepository;
  private final UserRepository userRepository;
  private final TenantContext tenantContext;

  @PostConstruct
  public void init() {
    tenantContext.runForEachTenant(tenantId -> {
      if (repository.count() == 0) {
        for (int i = 1; i <= 3; i++) {
          UniGroup group = new UniGroup();
          group.setName("Test" + i);
          group.setActive(true);
          group.setDescription("This is a test description " + i);
          group = repository.save(group);

          Department dep1 = new Department();
          dep1.setName("ΠΛΗΡΟΦΟΡΙΚΗ " + i);
          dep1.setDescription("Department description " + i);
          dep1.setActive(true);
          dep1.setGroup(group);
          dep1 = departmentRepository.save(dep1);

          Department dep2 = new Department();
          dep2.setName("ΦΥΣΙΚΟ " + i);
          dep2.setDescription("Department description " + i);
          dep2.setActive(true);
          dep2.setGroup(group);
          dep2 = departmentRepository.save(dep2);

          Department finalDep = dep1;
          userRepository.findByUsername("uniwa1_c1").ifPresent(user -> {
            user.setDepartment(finalDep);
            userRepository.save(user);
          });
        }
      }
    }, "GroupServiceImpl.init");
  }

  @Override
  protected void validate(String id, GroupDTO dto) {
    super.validate(id, dto);

    UniGroup existingGroup = repository.findByNameIgnoreCase(dto.getName()).orElse(null);
    if (existingGroup != null && !existingGroup.getId().equals(id)) {
      throw ExceptionUtils.createException(QAlreadyExistsException.class,
          "group_name_already_exists", "Group name {} already exists.", dto.getName());
    }
  }

  @Override
  public String create(GroupDTO dto) {
    return super.create(dto);
  }

  @Override
  public void update(String id, GroupDTO dto) {
    super.update(id, dto);
  }

  @Override
  public boolean setGroupStatus(String id, boolean active) {
    UniGroup group = readerService.findEntityById(id);

    // Don't update if status in entity is same with the requested.
    if (group.isActive() == active) {
      return group.isActive();
    }

    group.setActive(active);
    repository.save(group);

    return group.isActive();
  }

  @Override
  protected BaseReaderServiceImpl<GroupDTO, UniGroup> getReaderService() {
    return readerService;
  }

  @Override
  protected BaseUpdatableEntityMapper<GroupDTO, UniGroup> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<UniGroup> getRepository() {
    return repository;
  }
}
