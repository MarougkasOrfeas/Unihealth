package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.HealthProfile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthProfileRepository extends BaseRepository<HealthProfile> {

  Optional<HealthProfile> findByUserUsername(String username);

  Optional<HealthProfile> findByUserId(String userId);

  boolean existsByUserUsername(String username);

}
