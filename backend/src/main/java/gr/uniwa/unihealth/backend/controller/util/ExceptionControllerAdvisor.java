package gr.uniwa.unihealth.backend.controller.util;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.eurodyn.qlack.common.exception.QException;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.exception.UNIHEALTHException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advisor to handle HTTP response codes for various exceptions thrown.
 *
 * @author omaro
 */
@RestControllerAdvice
public class ExceptionControllerAdvisor {

  /**
   * Handles HTTP response codes for various exception types.
   *
   * @param exception The exception thrown.
   * @return An appropriate {@link ResponseEntity}.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleException(Exception exception) {
    Object responseEntity = null;
    Throwable toCheck = exception;

    if (exception instanceof UNIHEALTHException unihealthException) {
      responseEntity = unihealthException.getFrontendMessageLabel();
      toCheck = unihealthException.getCause();
    }

    ExceptionUtils.logException(exception);

    // Handle DTO validation errors
    if (toCheck instanceof MethodArgumentNotValidException exc) {
      return new ResponseEntity<>(exc.getBindingResult().getAllErrors(), HttpStatus.BAD_REQUEST);
    } else if (toCheck instanceof QDoesNotExistException /* TODO @Kostis || toCheck instanceof QFileNotFoundException */) {
      return new ResponseEntity<>(responseEntity, HttpStatus.NOT_FOUND);
    } else if (toCheck instanceof QException) {
      return new ResponseEntity<>(responseEntity, HttpStatus.BAD_REQUEST);
    } else if (toCheck instanceof AccessDeniedException) {
      return new ResponseEntity<>("error_core_401", HttpStatus.UNAUTHORIZED);
    } else if (toCheck instanceof ObjectOptimisticLockingFailureException) {
      return new ResponseEntity<>("concurrent_update_conflict", HttpStatus.CONFLICT);
    } else {
      return new ResponseEntity<>(responseEntity, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
