package gr.uniwa.unihealth.backend.mapper;

import com.eurodyn.qlack.fuse.lexicon.repository.LanguageRepository;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.model.User;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(config = MappingConfig.class)
public abstract class UserMapper extends BaseUpdatableEntityMapper<UserDTO, User> {

  @Autowired
  private LanguageRepository languageRepository;

  @Override
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "status",
      expression = "java(gr.uniwa.unihealth.backend.model.enums.UserStatus.UNVERIFIED)")
  @Mapping(target = "emailSentNoLoginSince", ignore = true)
  @Mapping(target = "language", ignore = true)
  public abstract User mapForCreate(UserDTO dto);

  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "emailSentNoLoginSince", ignore = true)
  @Mapping(target = "language", ignore = true)
  public abstract void mapForUpdate(UserDTO dto, @MappingTarget User entity);


  @Override
  @Mapping(target = "language", ignore = true)
  public abstract UserDTO mapToDTO(User entity);

  @AfterMapping
  protected void afterMapToDTO(User entity, @MappingTarget UserDTO dto) {
    dto.setLanguage(entity.getLanguage().getId());
  }

  @AfterMapping
  protected void afterMapToEntity(UserDTO dto, @MappingTarget User entity) {
    entity.setLanguage(languageRepository.getReferenceById(dto.getLanguage()));
  }

}
