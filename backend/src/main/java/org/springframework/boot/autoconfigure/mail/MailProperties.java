package org.springframework.boot.autoconfigure.mail;

/**
 * Compatibility shim for Spring Boot 4.0.
 * <p>
 * In Spring Boot 4.0, {@code MailProperties} was relocated from
 * {@code org.springframework.boot.autoconfigure.mail} to
 * {@code org.springframework.boot.mail.autoconfigure}. The qlack-fuse-mailing library (3.9.8) still
 * references the old package. This class bridges the gap by extending the new class at the old
 * package location.
 * </p>
 * <p>
 * Remove this class once qlack-fuse-mailing is updated for Spring Boot 4.0.
 * </p>
 *
 * @author omaro
 */
public class MailProperties extends org.springframework.boot.mail.autoconfigure.MailProperties {
}
