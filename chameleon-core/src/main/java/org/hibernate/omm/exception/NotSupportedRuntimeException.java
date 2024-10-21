package org.hibernate.omm.exception;

/**
 * Denotes that the feature in question is decided not to be supported by OMM
 *
 * @author Nathan Xu
 * @see NotYetImplementedException
 * @since 1.0.0
 */
public class NotSupportedRuntimeException extends RuntimeException {

    public NotSupportedRuntimeException() {}

    public NotSupportedRuntimeException(final String message) {
        super(message);
    }

    public NotSupportedRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NotSupportedRuntimeException(final Throwable cause) {
        super(cause);
    }
}
