package gr.uniwa.unihealth.backend.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import gr.uniwa.unihealth.backend.dto.BaseDTO;

public interface BaseReaderWithSearchService<D extends BaseDTO> extends BaseReaderService<D> {

  /**
   * Builds the search predicate for the given search text, locale, and search strategy.
   * Implementations define how free-text search is applied.
   *
   * @param params parameters for building the search predicate
   * @return search predicate
   */
  BooleanExpression buildSearchPredicate(BuildSearchPredicateParams params);

  /**
   * @param pickerMode True when the request comes from a record-picker dialog, so the column
   *                   configuration of the picker is read instead of the management table's.
   */
  record BuildSearchPredicateParams(String search, String locale, String searchStrategy,
                                    boolean pickerMode) {

    public BuildSearchPredicateParams(String search, String locale, String searchStrategy) {
      this(search, locale, searchStrategy, false);
    }
  }
}
