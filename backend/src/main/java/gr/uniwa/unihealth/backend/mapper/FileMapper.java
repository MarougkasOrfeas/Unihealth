package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.FileDTO;
import gr.uniwa.unihealth.backend.model.FileEntity;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public abstract class FileMapper extends BaseUpdatableEntityMapper<FileDTO, FileEntity> {

  /**
   * {@code userId} is mapped here on purpose. The only caller that reaches this is the uploading
   * service, which stamps the owner from the authenticated principal immediately beforehand — the
   * generic create routes are disabled on the file controller, so there is no path by which a
   * client-supplied value could arrive.
   */
  @Override
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "examYear", ignore = true)
  public abstract FileEntity mapForCreate(FileDTO dto);

  /**
   * A file never changes owner, type or size.
   *
   * <p>{@code contentType} and {@code fileSize} are already {@code updatable = false} at the JPA
   * level; {@code userId} joins them here so that an edit cannot re-home a document even if the
   * controller's ownership guard were ever removed. {@code examYear} is derived by the entity, so
   * letting a DTO write it would only let the two disagree.
   */
  @Override
  @InheritConfiguration(name = "mapDtoToUpdatableEntityConfig")
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "contentType", ignore = true)
  @Mapping(target = "fileSize", ignore = true)
  @Mapping(target = "examYear", ignore = true)
  public abstract void mapForUpdate(FileDTO dto, @MappingTarget FileEntity entity);
}
