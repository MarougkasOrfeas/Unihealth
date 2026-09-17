package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.model.OptionalHealthProfile;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.repository.OptionalHealthProfileRepository;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.EmailNotificationService;
import gr.uniwa.unihealth.backend.service.EmailPreferenceService;
import gr.uniwa.unihealth.backend.service.OptionalFormReminderService;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * Implementation for {@link OptionalFormReminderService}.
 *
 * <p>Driven by a scheduled job rather than a logout event: logout is handled entirely in the SPA by
 * `AuthService`, so the backend never learns about it, and a closed tab or an expired token would
 * skip the trigger altogether.
 *
 * <p>Two reminders, at least {@link #MIN_DAYS_BETWEEN_REMINDERS} days apart, ever. The cap lives in
 * the database ({@code optional_form_reminders_sent}) so a restart or a second instance cannot
 * restart the sequence.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OptionalFormReminderServiceImpl implements OptionalFormReminderService {

  /** Never send more than this many reminders to the same user, for the lifetime of the account. */
  private static final int MAX_REMINDERS = 2;

  private static final int MIN_DAYS_BETWEEN_REMINDERS = 3;

  /** Do not nudge an account until it has had a few days of use. */
  private static final int MIN_ACCOUNT_AGE_DAYS = 3;

  /** A profile at or above this much completeness is left alone. */
  private static final double COMPLETE_ENOUGH_RATIO = 0.5;

  /**
   * The fields that always apply, so they can be counted fairly.
   *
   * <p>`medicationDetails` and `surgeryDetails` are deliberately excluded: they only apply when
   * their boolean is true, so counting them would leave most profiles permanently "half empty".
   * `comments` is excluded for the same reason — it is free text nobody is expected to fill.
   */
  private static final List<Function<OptionalHealthProfile, Object>> COUNTED_FIELDS = List.of(
      OptionalHealthProfile::getSleepQuality,
      OptionalHealthProfile::getStudyLoad,
      OptionalHealthProfile::getSmoking,
      OptionalHealthProfile::getCoffee,
      OptionalHealthProfile::getScreenTime,
      OptionalHealthProfile::getExercise,
      OptionalHealthProfile::getMealsPerDay,
      OptionalHealthProfile::getEatSnack,
      OptionalHealthProfile::getWater,
      OptionalHealthProfile::getDietType,
      OptionalHealthProfile::getMedication,
      OptionalHealthProfile::getSurgeryHistory,
      OptionalHealthProfile::getPreferredContent,
      OptionalHealthProfile::getFrequency);

  private final UserRepository userRepository;
  private final OptionalHealthProfileRepository optionalHealthProfileRepository;
  private final EmailNotificationService emailNotificationService;
  private final EmailPreferenceService emailPreferenceService;

  @Value("${unihealth.app.www.url}")
  private String appUrl;

  @Override
  public int remindIncompleteOptionalProfiles() {
    LocalDateTime now = LocalDateTime.now();

    List<User> candidates = userRepository.findRemindableForOptionalForm(UserStatus.ACTIVE,
        MAX_REMINDERS, now.minusDays(MIN_DAYS_BETWEEN_REMINDERS),
        now.minusDays(MIN_ACCOUNT_AGE_DAYS));

    if (CollectionUtils.isEmpty(candidates)) {
      log.info("No users to remind about the optional health profile.");
      return 0;
    }

    int sent = 0;
    for (User user : candidates) {
      // Re-checked per user rather than in the query: the preference gate is the single place that
      // decides what may be sent, and it is what keeps account-critical mail unmutable.
      if (!emailPreferenceService.mayReceive(user, EmailNotificationType.OPTIONAL_FORM_REMINDER)) {
        continue;
      }

      if (filledRatio(user) >= COMPLETE_ENOUGH_RATIO) {
        continue;
      }

      emailNotificationService.sendEmailNotification(List.of(user.getEmail()), localeOf(user),
          EmailNotificationType.OPTIONAL_FORM_REMINDER, appUrl);

      user.setOptionalFormRemindersSent(user.getOptionalFormRemindersSent() + 1);
      user.setOptionalFormReminderLastSentOn(now);
      userRepository.save(user);
      sent++;
    }

    return sent;
  }

  /**
   * How much of the optional profile is filled, as a fraction of {@link #COUNTED_FIELDS}. A user
   * who never opened the form has no row at all, which counts as nothing filled.
   */
  private double filledRatio(User user) {
    OptionalHealthProfile profile =
        optionalHealthProfileRepository.findByHealthProfileUserId(user.getId()).orElse(null);

    if (profile == null) {
      return 0d;
    }

    long filled = COUNTED_FIELDS.stream().filter(getter -> getter.apply(profile) != null).count();
    return (double) filled / COUNTED_FIELDS.size();
  }

  private String localeOf(User user) {
    return user.getLanguage() != null ? user.getLanguage().getLocale() : "el";
  }
}
