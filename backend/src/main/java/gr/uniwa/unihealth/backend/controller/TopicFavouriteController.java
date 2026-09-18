package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.personalization.UserFavouriteTopicService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Favourite health topics of the logged in user.
 *
 * Every endpoint is `_me` scoped and resolves the user from the authenticated principal, never
 * from the request, so there is no way to address another user's favourites. A list of topic ids
 * is the entire payload, which is why there is no DTO or mapper.
 */
@RestController
@RequestMapping("topic-favourite")
@RequiredArgsConstructor
public class TopicFavouriteController {

  private final UserFavouriteTopicService service;
  private final UserReaderService userReaderService;

  @GetMapping("_me")
  @Operation(summary = "Returns the favourite topic ids of the logged in user")
  public List<String> findMyFavourites() {
    return service.findForUser(currentUserId());
  }

  @PutMapping("_me/{topicId}")
  @Operation(summary = "Marks a topic as a favourite for the logged in user",
      description = "Idempotent: marking an already favourited topic succeeds and changes nothing.")
  public void addFavourite(@PathVariable String topicId) {
    service.add(currentUserId(), topicId);
  }

  @DeleteMapping("_me/{topicId}")
  @Operation(summary = "Removes a topic from the favourites of the logged in user")
  public void removeFavourite(@PathVariable String topicId) {
    service.remove(currentUserId(), topicId);
  }

  private String currentUserId() {
    return userReaderService.findLoggedInUser().getId();
  }
}
