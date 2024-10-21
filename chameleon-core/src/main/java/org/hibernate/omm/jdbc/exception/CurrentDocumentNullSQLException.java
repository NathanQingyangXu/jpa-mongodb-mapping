package org.hibernate.omm.jdbc.exception;

public class CurrentDocumentNullSQLException extends SimulatedSQLException {
    public CurrentDocumentNullSQLException() {}

    public CurrentDocumentNullSQLException(final String reason) {
        super(reason);
    }

    public CurrentDocumentNullSQLException(final String reason, final Throwable cause) {
        super(reason, cause);
    }
}
