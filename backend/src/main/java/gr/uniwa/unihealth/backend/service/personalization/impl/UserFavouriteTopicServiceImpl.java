package gr.uniwa.unihealth.backend.service.personalization.impl;

import gr.uniwa.unihealth.backend.model.UserFavouriteTopic;
import gr.uniwa.unihealth.backend.repository.UserFavouriteTopicRepository;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.personalization.UserFavouriteTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFavouriteTopicServiceImpl implements UserFavouriteTopicService {

  private final UserFavouriteTopicRepository repository;
  private final UserRepository userRepository;

  @Override
  public List<String> findForUser(String userId) {
    return repository.findByUserId(userId).stream()
        .map(UserFavouriteTopic::getTopicId)
        .toList();
  }

  @Override
  @Transactional
  public void add(String userId, String topicId) {
    if (repository.existsByUserIdAndTopicId(userId, topicId)) {
      return;
    }

    UserFavouriteTopic favourite = new UserFavouriteTopic();
    // A reference, not a load: the id already comes from the authenticated principal, so there is
    // nothing to validate and no reason to pay for the select.
    favourite.setUser(userRepository.getReferenceById(userId));
    favourite.setTopicId(topicId);

    repository.save(favourite);
  }

  @Override
  @Transactional
  public void remove(String userId, String topicId) {
    repository.deleteByUserIdAndTopicId(userId, topicId);
  }
}
