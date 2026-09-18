package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.UserLabelMetric;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLabelMetricRepository extends BaseRepository<UserLabelMetric> {

  List<UserLabelMetric> findByUserId(String userId);

  Optional<UserLabelMetric> findByUserIdAndLabelCode(String userId, String labelCode);

  /** Used when consent is withdrawn: collection stops and everything already held is removed. */
  void deleteByUserId(String userId);
}
