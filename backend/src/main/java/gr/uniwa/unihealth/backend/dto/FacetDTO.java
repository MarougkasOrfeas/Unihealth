package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class FacetDTO {

  /**
   * The text term used to filter the returned available options. Use with contains
   */
  private String columnFilter;

  /**
   * The name of the column/field to load the options. Will use the FE UI_BACKEND_MAPPING table same
   * as the filters.
   */
  private String column;

  /**
   * The global search term used to narrow all the entries. (Suchen field)
   */
  private String search;

  private String locale;

  private String searchStrategy;

  /**
   * True when the request comes from a record-picker dialog, so the search predicate is built from
   * the picker's column configuration instead of the management table's.
   */
  private boolean pickerMode;

  /**
   * The map used to do the global entries filtering. To be used to create the predicate to filter
   * the DB entries similar to _page mapping.
   */
  private Map<String, Object> predicateParams = new HashMap<>();
}
