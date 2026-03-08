package gr.uniwa.unihealth.backend.exception;

import lombok.Getter;

/**
 * A generic exception specific to this application.
 *
 * @author omaro
 */
@Getter
public class UNIHEALTHException extends RuntimeException {

  private final String frontendMessageLabel;

  /**
   * Constructor.
   *
   * @param frontendMessageLabel The label for the message to be displayed on front end.
   * @param message              The exception message
   * @param cause                The cause of this exception.
   */
  public UNIHEALTHException(String frontendMessageLabel, String message, Throwable cause) {
    super(message, cause);
    this.frontendMessageLabel = frontendMessageLabel;
  }
}
