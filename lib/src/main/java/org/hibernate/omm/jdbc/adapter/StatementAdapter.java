package org.hibernate.omm.jdbc.adapter;

import java.sql.*;

import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;

public interface StatementAdapter extends Statement {

	@Override
	public default ResultSet executeQuery(String sql) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int executeUpdate(String sql) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void close() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int getMaxFieldSize() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void setMaxFieldSize(int max) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int getMaxRows() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void setMaxRows(int max) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void setEscapeProcessing(boolean enable) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int getQueryTimeout() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void setQueryTimeout(int seconds) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void cancel() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default SQLWarning getWarnings() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void clearWarnings() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void setCursorName(String name) throws SimulatedSQLException {
	}

	@Override
	public default boolean execute(String sql) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default ResultSet getResultSet() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int getUpdateCount() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default boolean getMoreResults() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void setFetchDirection(int direction) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int getFetchDirection() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void setFetchSize(int rows) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int getFetchSize() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int getResultSetConcurrency() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int getResultSetType() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void addBatch(String sql) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void clearBatch() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int[] executeBatch() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default Connection getConnection() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default boolean getMoreResults(int current) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default ResultSet getGeneratedKeys() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int executeUpdate(String sql, int autoGeneratedKeys) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int executeUpdate(String sql, int[] columnIndexes) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int executeUpdate(String sql, String[] columnNames) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default boolean execute(String sql, int autoGeneratedKeys) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default boolean execute(String sql, int[] columnIndexes) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default boolean execute(String sql, String[] columnNames) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default int getResultSetHoldability() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default boolean isClosed() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void setPoolable(boolean poolable) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default boolean isPoolable() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default void closeOnCompletion() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default boolean isCloseOnCompletion() throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default <T> T unwrap(Class<T> iface) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}

	@Override
	public default boolean isWrapperFor(Class<?> iface) throws SimulatedSQLException {
		throw new NotSupportedSQLException();
	}
}
