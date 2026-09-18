package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.UserFavouriteTopic;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavouriteTopicRepository extends BaseRepository<UserFavouriteTopic> {

  List<UserFavouriteTopic> findByUserId(String userId);

  boolean existsByUserIdAndTopicId(String userId, String topicId);

  void deleteByUserIdAndTopicId(String userId, String topicId);
}
