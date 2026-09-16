package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.ConditionDTO;
import gr.uniwa.unihealth.backend.dto.ConditionDetailDTO;
import gr.uniwa.unihealth.backend.dto.SymptomItemDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.ConditionMapper;
import gr.uniwa.unihealth.backend.mapper.SymptomItemMapper;
import gr.uniwa.unihealth.backend.model.Condition;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.ConditionRepository;
import gr.uniwa.unihealth.backend.repository.SymptomItemRepository;
import gr.uniwa.unihealth.backend.service.ConditionReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConditionReaderServiceImpl extends BaseReaderServiceImpl<ConditionDTO, Condition>
    implements ConditionReaderService {

  private final ConditionRepository repository;
  private final SymptomItemRepository symptomRepository;
  private final ConditionMapper mapper;
  private final SymptomItemMapper symptomMapper;

  @Override
  public List<ConditionDTO> findContent(String search) {
    List<Condition> items;

    if (search == null || search.isBlank()) {
      items = repository.findByActiveTrueOrderByStartingLetterAscDisplayOrderAscNameAsc();
    } else {
      items = repository.searchActive(search.trim());
    }

    return items.stream().map(mapper::mapToDTO).toList();
  }

  @Override
  public ConditionDetailDTO findBySlug(String slug) {
    Condition entity =
        repository.findBySlugAndActiveTrue(slug).orElseThrow(QDoesNotExistException::new);

    ConditionDetailDTO detail = mapper.mapToDetailDTO(entity);

    // The condition pages are never ingested, so what we can usefully show instead is the set of
    // symptoms that lead here.
    List<SymptomItemDTO> related = repository.findSymptomSlugsForCondition(slug).stream()
        .map(symptomRepository::findBySlugAndActiveTrue)
        .flatMap(Optional::stream)
        .map(symptomMapper::mapToDTO)
        .toList();
    detail.setRelatedSymptoms(related);

    return detail;
  }

  @Override
  protected BaseEntityMapper<ConditionDTO, Condition> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<Condition> getRepository() {
    return repository;
  }
}
