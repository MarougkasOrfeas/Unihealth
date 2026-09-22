package gr.uniwa.unihealth.backend.mapper;

import gr.uniwa.unihealth.backend.dto.UserSurveyDTO;
import gr.uniwa.unihealth.backend.model.UserSurvey;
import org.mapstruct.Mapper;

/**
 * Empty on purpose.
 *
 * <p>{@link BaseUpdatableEntityMapper} supplies {@code mapForUpdate} and {@code mapToDTO}, and
 * {@link MappingConfig} already ignores the id and every audit field — so overwriting a response
 * cannot disturb its identity or its history. The nine enum answers convert from their names by
 * MapStruct's own String-to-enum handling, with no mapping to declare.
 */
@Mapper(config = MappingConfig.class)
public abstract class UserSurveyMapper
    extends BaseUpdatableEntityMapper<UserSurveyDTO, UserSurvey> {
}
