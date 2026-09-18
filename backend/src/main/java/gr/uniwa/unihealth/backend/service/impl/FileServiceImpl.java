package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.FileDTO;
import gr.uniwa.unihealth.backend.mapper.BaseUpdatableEntityMapper;
import gr.uniwa.unihealth.backend.mapper.FileMapper;
import gr.uniwa.unihealth.backend.model.FileEntity;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.FileRepository;
import gr.uniwa.unihealth.backend.service.FileHandlerService;
import gr.uniwa.unihealth.backend.service.FileService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileServiceImpl extends BaseUpdatableServiceImpl<FileDTO, FileEntity>
    implements FileService {

  private final FileReaderServiceImpl readerService;

  private final FileMapper mapper;

  private final FileRepository repository;

  private final FileHandlerService fileHandlerService;

  @Override
  public String create(FileDTO dto) {
    String id = super.create(dto);
    repository.flush();

    fileHandlerService.uploadFile(id, dto.getContentType(), dto.getFileSize(),
        () -> dto.getMultipartFile().getInputStream());

    return id;
  }

  @Override
  public void delete(String id) {
    super.delete(id);
    repository.flush();
    fileHandlerService.deleteFile(id);
  }

  @Override
  public void delete(Collection<String> ids) {
    List<String> fileIds =
        CollectionUtils.emptyIfNull(ids).stream().filter(StringUtils::isNotBlank).distinct()
            .toList();
    if (fileIds.isEmpty()) {
      return;
    }

    // Only the records
    fileIds.forEach(super::delete);
    repository.flush();

    // Registers the stored content for removal after the commit, so a failure part-way through
    // cannot leave records whose content is already gone.
    fileHandlerService.deleteFiles(fileIds);
  }

  @Override
  public InputStream getFile(String id) {
    return fileHandlerService.getFile(id);
  }

  @Override
  public void validateAvailable(String id, FileDTO dto) {
  }

  @Override
  protected BaseReaderServiceImpl<FileDTO, FileEntity> getReaderService() {
    return readerService;
  }

  @Override
  protected BaseUpdatableEntityMapper<FileDTO, FileEntity> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<FileEntity> getRepository() {
    return repository;
  }
}
