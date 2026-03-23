package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.UserProfileLabels;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileLabelsRepository extends BaseRepository<UserProfileLabels>{

  Optional<UserProfileLabels> findByUserId(String userId);
}
