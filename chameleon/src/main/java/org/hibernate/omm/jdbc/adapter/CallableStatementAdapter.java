package org.hibernate.omm.jdbc.adapter;

import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public interface CallableStatementAdapter extends CallableStatement {

    @Override
    default void registerOutParameter(int parameterIndex, int sqlType) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void registerOutParameter(int parameterIndex, int sqlType, int scale)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean wasNull() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default String getString(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean getBoolean(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default byte getByte(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default short getShort(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getInt(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default long getLong(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default float getFloat(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default double getDouble(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default BigDecimal getBigDecimal(int parameterIndex, int scale) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default byte[] getBytes(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Date getDate(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Time getTime(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Timestamp getTimestamp(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Object getObject(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default BigDecimal getBigDecimal(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Object getObject(int parameterIndex, Map<String, Class<?>> map)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Ref getRef(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Blob getBlob(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Clob getClob(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Array getArray(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Date getDate(int parameterIndex, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Time getTime(int parameterIndex, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void registerOutParameter(int parameterIndex, int sqlType, String typeName)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void registerOutParameter(String parameterName, int sqlType) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void registerOutParameter(String parameterName, int sqlType, int scale)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void registerOutParameter(String parameterName, int sqlType, String typeName)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default URL getURL(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setURL(String parameterName, URL val) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNull(String parameterName, int sqlType) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBoolean(String parameterName, boolean x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setByte(String parameterName, byte x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setShort(String parameterName, short x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setInt(String parameterName, int x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setLong(String parameterName, long x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setFloat(String parameterName, float x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setDouble(String parameterName, double x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBigDecimal(String parameterName, BigDecimal x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setString(String parameterName, String x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBytes(String parameterName, byte[] x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setDate(String parameterName, Date x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setTime(String parameterName, Time x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setTimestamp(String parameterName, Timestamp x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setAsciiStream(String parameterName, InputStream x, int length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBinaryStream(String parameterName, InputStream x, int length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setObject(String parameterName, Object x, int targetSqlType, int scale)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setObject(String parameterName, Object x, int targetSqlType)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setObject(String parameterName, Object x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setCharacterStream(String parameterName, Reader reader, int length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setDate(String parameterName, Date x, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setTime(String parameterName, Time x, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setTimestamp(String parameterName, Timestamp x, Calendar cal)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNull(String parameterName, int sqlType, String typeName)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default String getString(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean getBoolean(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default byte getByte(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default short getShort(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int getInt(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default long getLong(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default float getFloat(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default double getDouble(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default byte[] getBytes(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Date getDate(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Time getTime(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Timestamp getTimestamp(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Object getObject(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default BigDecimal getBigDecimal(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Object getObject(String parameterName, Map<String, Class<?>> map)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Ref getRef(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Blob getBlob(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Clob getClob(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Array getArray(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Date getDate(String parameterName, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Time getTime(String parameterName, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Timestamp getTimestamp(String parameterName, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default URL getURL(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default RowId getRowId(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default RowId getRowId(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setRowId(String parameterName, RowId x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNString(String parameterName, String value) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNCharacterStream(String parameterName, Reader value, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNClob(String parameterName, NClob value) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setClob(String parameterName, Reader reader, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBlob(String parameterName, InputStream inputStream, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNClob(String parameterName, Reader reader, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default NClob getNClob(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default NClob getNClob(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setSQLXML(String parameterName, SQLXML xmlObject) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default SQLXML getSQLXML(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default SQLXML getSQLXML(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default String getNString(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default String getNString(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Reader getNCharacterStream(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Reader getNCharacterStream(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Reader getCharacterStream(int parameterIndex) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Reader getCharacterStream(String parameterName) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBlob(String parameterName, Blob x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setClob(String parameterName, Clob x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setAsciiStream(String parameterName, InputStream x, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBinaryStream(String parameterName, InputStream x, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setCharacterStream(String parameterName, Reader reader, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setAsciiStream(String parameterName, InputStream x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBinaryStream(String parameterName, InputStream x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setCharacterStream(String parameterName, Reader reader) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNCharacterStream(String parameterName, Reader value) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setClob(String parameterName, Reader reader) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBlob(String parameterName, InputStream inputStream) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNClob(String parameterName, Reader reader) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default <T> T getObject(int parameterIndex, Class<T> type) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default <T> T getObject(String parameterName, Class<T> type) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default ResultSet executeQuery() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default int executeUpdate() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNull(int parameterIndex, int sqlType) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBoolean(int parameterIndex, boolean x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setByte(int parameterIndex, byte x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setShort(int parameterIndex, short x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setInt(int parameterIndex, int x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setLong(int parameterIndex, long x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setFloat(int parameterIndex, float x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setDouble(int parameterIndex, double x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBigDecimal(int parameterIndex, BigDecimal x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setString(int parameterIndex, String x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBytes(int parameterIndex, byte[] x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setDate(int parameterIndex, Date x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setTime(int parameterIndex, Time x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setTimestamp(int parameterIndex, Timestamp x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setAsciiStream(int parameterIndex, InputStream x, int length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setUnicodeStream(int parameterIndex, InputStream x, int length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBinaryStream(int parameterIndex, InputStream x, int length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void clearParameters() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setObject(int parameterIndex, Object x, int targetSqlType)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setObject(int parameterIndex, Object x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean execute() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void addBatch() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setCharacterStream(int parameterIndex, Reader reader, int length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setRef(int parameterIndex, Ref x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBlob(int parameterIndex, Blob x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setClob(int parameterIndex, Clob x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setArray(int parameterIndex, Array x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default ResultSetMetaData getMetaData() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setDate(int parameterIndex, Date x, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setTime(int parameterIndex, Time x, Calendar cal) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNull(int parameterIndex, int sqlType, String typeName)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setURL(int parameterIndex, URL x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default ParameterMetaData getParameterMetaData() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setRowId(int parameterIndex, RowId x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNString(int parameterIndex, String value) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNCharacterStream(int parameterIndex, Reader value, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNClob(int parameterIndex, NClob value) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setClob(int parameterIndex, Reader reader, long length) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBlob(int parameterIndex, InputStream inputStream, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNClob(int parameterIndex, Reader reader, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setAsciiStream(int parameterIndex, InputStream x, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBinaryStream(int parameterIndex, InputStream x, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setCharacterStream(int parameterIndex, Reader reader, long length)
            throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setAsciiStream(int parameterIndex, InputStream x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBinaryStream(int parameterIndex, InputStream x) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setCharacterStream(int parameterIndex, Reader reader) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNCharacterStream(int parameterIndex, Reader value) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setClob(int parameterIndex, Reader reader) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setBlob(int parameterIndex, InputStream inputStream) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void setNClob(int parameterIndex, Reader reader) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

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
        throw new NotSupportedSQLException();
    }

    @Override
    default boolean execute(String sql) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
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
