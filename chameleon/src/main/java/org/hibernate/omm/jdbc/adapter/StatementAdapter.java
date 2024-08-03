package org.hibernate.omm.jdbc.adapter;

import com.mongodb.lang.Nullable;
import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public interface StatementAdapter extends Statement {

    @Override
    default ResultSet executeQuery(String sql) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int executeUpdate(String sql) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void close() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getMaxFieldSize() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setMaxFieldSize(int max) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getMaxRows() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setMaxRows(int max) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setEscapeProcessing(boolean enable) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getQueryTimeout() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setQueryTimeout(int seconds) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void cancel() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default SQLWarning getWarnings() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void clearWarnings() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setCursorName(String name) throws SimulatedSQLException {
    }

    @Override
    default boolean execute(String sql) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    @Nullable
    default ResultSet getResultSet() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getUpdateCount() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean getMoreResults() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setFetchDirection(int direction) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getFetchDirection() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setFetchSize(int rows) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getFetchSize() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getResultSetConcurrency() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getResultSetType() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void addBatch(String sql) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void clearBatch() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int[] executeBatch() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Connection getConnection() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean getMoreResults(int current) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default ResultSet getGeneratedKeys() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int executeUpdate(String sql, int autoGeneratedKeys) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int executeUpdate(String sql, int[] columnIndexes) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int executeUpdate(String sql, String[] columnNames) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean execute(String sql, int autoGeneratedKeys) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean execute(String sql, int[] columnIndexes) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean execute(String sql, String[] columnNames) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getResultSetHoldability() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean isClosed() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setPoolable(boolean poolable) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean isPoolable() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void closeOnCompletion() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean isCloseOnCompletion() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default <T> T unwrap(Class<T> iface) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean isWrapperFor(Class<?> iface) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }
}
