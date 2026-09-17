package gr.uniwa.unihealth.backend.service;

/**
 * Nudges users whose optional health profile is still largely empty.
 */
public interface OptionalFormReminderService {

  /**
   * Sends at most one reminder per eligible user per run.
   *
   * @return how many reminders were sent.
   */
  int remindIncompleteOptionalProfiles();
}
