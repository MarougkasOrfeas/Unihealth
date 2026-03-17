package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QAlreadyExistsException;
import gr.uniwa.unihealth.backend.dto.GroupDTO;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.mapper.BaseUpdatableEntityMapper;
import gr.uniwa.unihealth.backend.mapper.UniGroupMapper;
import gr.uniwa.unihealth.backend.model.UniGroup;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.UniGroupRepository;
import gr.uniwa.unihealth.backend.service.GroupService;
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
