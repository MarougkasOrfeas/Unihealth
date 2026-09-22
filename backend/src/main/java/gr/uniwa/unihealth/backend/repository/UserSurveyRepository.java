package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.UserSurvey;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSurveyRepository extends BaseRepository<UserSurvey> {

  /**
   * The only lookup this table needs: the caller's own response.
   *
   * <p>By username rather than by id, because that is what the JWT carries — the write path would
   * otherwise pay for a user select just to turn one identifier into another.
   */
  Optional<UserSurvey> findByUserUsername(String username);
}
