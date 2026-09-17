package gr.uniwa.unihealth.backend.service.util;

import gr.uniwa.unihealth.backend.model.BaseEntity;
import gr.uniwa.unihealth.backend.model.config.EntityFilterConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EntityFilterConfigRegistry {

  private final Map<Class<?>, EntityFilterConfig<?>> configs;

  public EntityFilterConfigRegistry(List<EntityFilterConfig<?>> configList) {
    this.configs = configList.stream()
        .collect(Collectors.toMap(EntityFilterConfig::entityType, Function.identity()));
  }

  @SuppressWarnings("unchecked")
  public <E extends BaseEntity> EntityFilterConfig<E> getConfig(Class<E> entityType) {
    return (EntityFilterConfig<E>) configs.get(entityType);
  }
}

