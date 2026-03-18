package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.OptionalHealthProfile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OptionalHealthProfileRepository extends BaseRepository<OptionalHealthProfile>{

  Optional<OptionalHealthProfile> findByHealthProfileUserUsername(String username);

  Optional<OptionalHealthProfile> findByHealthProfileId(String healthProfileId);
}
