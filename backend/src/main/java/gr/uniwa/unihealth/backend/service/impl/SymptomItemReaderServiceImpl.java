package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.SymptomItemDTO;
import gr.uniwa.unihealth.backend.dto.SymptomItemDetailDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.SymptomItemMapper;
import gr.uniwa.unihealth.backend.model.SymptomItem;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.SymptomItemRepository;
import gr.uniwa.unihealth.backend.service.SymptomItemReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SymptomItemReaderServiceImpl extends BaseReaderServiceImpl<SymptomItemDTO, SymptomItem>
    implements SymptomItemReaderService {

  private final SymptomItemRepository repository;
  private final SymptomItemMapper mapper;

  @Override
  public List<SymptomItemDTO> findContent(String search) {
    List<SymptomItem> items;

    if (search == null || search.isBlank()) {
      items = repository.findByActiveTrueOrderByStartingLetterAscDisplayOrderAscTitleAsc();
    } else {
      items = repository.searchActive(search.trim());
    }

    return items.stream().map(mapper::mapToDTO).toList();
  }

  @Override
  public SymptomItemDetailDTO findBySlug(String slug) {
    SymptomItem entity =
        repository.findBySlugAndActiveTrue(slug).orElseThrow(QDoesNotExistException::new);

    return mapper.mapToDetailDTO(entity);
  }

  @Override
  protected BaseEntityMapper<SymptomItemDTO, SymptomItem> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<SymptomItem> getRepository() {
    return repository;
  }
}
