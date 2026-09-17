package gr.uniwa.unihealth.backend.model.config;


import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.model.enums.FilterMode;

import java.util.Map;

public interface EntityFilterConfig<E extends BaseEntity> {

  Class<E> entityType();

  Map<String, FilterMode> filterModeMap();

  default FilterMode defaultStringMode() {
    return FilterMode.EQUALS_IGNORE_CASE;
  }

}
