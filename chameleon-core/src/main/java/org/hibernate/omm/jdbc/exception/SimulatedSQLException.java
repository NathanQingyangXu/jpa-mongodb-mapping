package org.hibernate.omm.jdbc.exception;

import com.mongodb.lang.Nullable;
import java.sql.SQLException;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class SimulatedSQLException extends SQLException {

    public SimulatedSQLException() {}

    public SimulatedSQLException(@Nullable final String reason) {
        super(reason);
    }

    public SimulatedSQLException(@Nullable final String reason, final Throwable cause) {
        super(reason, cause);
    }
}
