package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QAlreadyExistsException;
import gr.uniwa.unihealth.backend.dto.DepartmentDTO;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.mapper.BaseUpdatableEntityMapper;
import gr.uniwa.unihealth.backend.mapper.DepartmentMapper;
import gr.uniwa.unihealth.backend.model.Department;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.DepartmentRepository;
import gr.uniwa.unihealth.backend.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends BaseUpdatableServiceImpl<DepartmentDTO, Department>
    implements DepartmentService {

  private final DepartmentReaderServiceImpl readerService;
  private final DepartmentMapper mapper;
  private final DepartmentRepository repository;

  @Override
  protected void validate(String id, DepartmentDTO dto) {
    super.validate(id, dto);

    Department existingDepartment = repository.findByNameIgnoreCase(dto.getName()).orElse(null);
    if (existingDepartment != null && !existingDepartment.getId().equals(id)) {
      throw ExceptionUtils.createException(QAlreadyExistsException.class,
          "department_name_already_exists", "Department name {} already exists.", dto.getName());
    }
  }

  @Override
  public String create(DepartmentDTO dto) {
    return super.create(dto);
  }

  @Override
  public void update(String id, DepartmentDTO dto) {
    super.update(id, dto);
  }

  @Override
  public boolean setGroupStatus(String id, boolean active) {
    return false;
  }

  @Override
  protected BaseReaderServiceImpl<DepartmentDTO, Department> getReaderService() {
    return readerService;
  }

  @Override
  protected BaseUpdatableEntityMapper<DepartmentDTO, Department> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<Department> getRepository() {
    return repository;
  }
}
