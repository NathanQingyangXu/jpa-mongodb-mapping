package org.hibernate.omm.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;
import org.hibernate.omm.jdbc.adapter.PreparedStatementAdapter;
import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;

public class MongodbPreparedStatement extends PreparedStatementAdapter {

  @Override
  public ResultSet executeQuery() throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public int executeUpdate() throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setNull(int parameterIndex, int sqlType) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setByte(int parameterIndex, byte x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setShort(int parameterIndex, short x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setInt(int parameterIndex, int x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setLong(int parameterIndex, long x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setFloat(int parameterIndex, float x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setDouble(int parameterIndex, double x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setString(int parameterIndex, String x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setDate(int parameterIndex, Date x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setTime(int parameterIndex, Time x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setObject(int parameterIndex, Object x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public boolean execute() throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void addBatch() throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setBlob(int parameterIndex, Blob x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setClob(int parameterIndex, Clob x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setArray(int parameterIndex, Array x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setNString(int parameterIndex, String value) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length)
      throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setNClob(int parameterIndex, NClob value) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length)
      throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    throw new NotSupportedSQLException();
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length)
      throws SQLException {
    throw new NotSupportedSQLException();
  }
}
