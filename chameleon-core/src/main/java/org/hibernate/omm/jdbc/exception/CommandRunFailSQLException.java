package org.hibernate.omm.jdbc.exception;

import org.bson.Document;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class CommandRunFailSQLException extends SimulatedSQLException {

    private final int code;
    private final String codeName;

    public CommandRunFailSQLException(final Document response) {
        super(response.getString("errmsg"));
        this.code = response.getInteger("code");
        this.codeName = response.getString("codeName");
    }

    public int getCode() {
        return code;
    }

    public String getCodeName() {
        return codeName;
    }
}
