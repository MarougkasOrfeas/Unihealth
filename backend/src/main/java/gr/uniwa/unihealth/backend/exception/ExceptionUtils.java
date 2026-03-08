package gr.uniwa.unihealth.backend.exception;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import java.lang.reflect.Constructor;

/**
 * Utility class that provides functions related to exceptions.
 *
 * @author omaro
 */
@Slf4j
@UtilityClass
public class ExceptionUtils {

  /**
   * Creates a {@link RuntimeException}.
   *
   * @param <T>                  The type of the exception to create.
   * @param exceptionClass       The {@link Class} of the exception to create.
   * @param frontendMessageLabel A message label for the front end application.
   * @param errorMessageFormat   The message for the exception.
   * @param errorMessageParams   Parameters to for the message.
   * @return The Exception created.
   */
  public <T extends RuntimeException> RuntimeException createException(Class<T> exceptionClass,
      String frontendMessageLabel, String errorMessageFormat, Object... errorMessageParams) {

    String errorMessage =
        MessageFormatter.arrayFormat(errorMessageFormat, errorMessageParams).getMessage();

    try {
      Constructor<T> constructor = exceptionClass.getConstructor(String.class);
      T exception = constructor.newInstance(errorMessage);

      return StringUtils.isBlank(frontendMessageLabel) ?
          exception :
          new UNIHEALTHException(frontendMessageLabel, errorMessage, exception);
    } catch (ReflectiveOperationException | IllegalArgumentException e) {
      throw new IllegalStateException("Could not instantiate " + exceptionClass.getName(), e);
    }
  }

  /**
   * Logs a throwable.
   *
   * @param throwable The throwable to log.
   */
  public void logException(Throwable throwable) {
    log.error(throwable.getMessage(), throwable);
  }
}
