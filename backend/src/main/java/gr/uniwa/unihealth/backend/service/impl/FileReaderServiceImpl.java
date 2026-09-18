package gr.uniwa.unihealth.backend.service.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import gr.uniwa.unihealth.backend.dto.FileDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.FileMapper;
import gr.uniwa.unihealth.backend.model.FileEntity;
import gr.uniwa.unihealth.backend.model.enums.MedicalTestCategory;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.FileRepository;
import gr.uniwa.unihealth.backend.service.FileReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static gr.uniwa.unihealth.backend.model.QFileEntity.fileEntity;


@Service
@RequiredArgsConstructor
public class FileReaderServiceImpl extends BaseReaderServiceImpl<FileDTO, FileEntity>
    implements FileReaderService {

  private final FileMapper mapper;

  private final FileRepository repository;

  /**
   * The columns the search box looks through — that is, the ones the list actually shows.
   *
   * <p>Three deliberate omissions. {@code active} is a boolean and the field-search helper has no
   * case for one, so it is matched by its label further down instead. {@code fileSize} is a byte
   * count, where a substring match would make "2026" find a 12026-byte file. {@code createdBy} is
   * always the person searching, so it can only ever match everything or nothing.
   */
  private static final List<SimpleExpression<?>> DEFAULT_COLUMNS =
      List.of(fileEntity.name, fileEntity.description, fileEntity.contentType, fileEntity.category,
          fileEntity.examDate, fileEntity.createdOn);


  /**
   * Note what this used to do: match the literal words "available" and "unavailable" and nothing
   * else, while {@code DEFAULT_COLUMNS} sat unused. Searching for a file by its name — the only
   * thing anyone would type — returned nothing.
   */
  @Override
  public BooleanExpression buildSearchPredicate(BuildSearchPredicateParams params) {
    String search = params.search();
    String locale = params.locale();

    List<BooleanExpression> predicates = new ArrayList<>();
    predicates.add(queryUtils.buildFieldSearchPredicate(DEFAULT_COLUMNS, search));

    if (super.translatedLabelMatches("global.file.available", locale, search)) {
      predicates.add(fileEntity.active.isTrue());
    }
    if (super.translatedLabelMatches("global.file.unavailable", locale, search)) {
      predicates.add(fileEntity.active.isFalse());
    }

    // The category is stored as the enum name but shown translated, so someone typing
    // «Αιματολογική» has to reach BLOOD_TEST.
    for (MedicalTestCategory category : MedicalTestCategory.values()) {
      if (super.translatedLabelMatches(category.getLabelCode(), locale, search)) {
        predicates.add(fileEntity.category.eq(category));
      }
    }

    return queryUtils.anyOfOrNoMatch(predicates);
  }


  @Override
  protected BaseEntityMapper<FileDTO, FileEntity> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<FileEntity> getRepository() {
    return repository;
  }
}
