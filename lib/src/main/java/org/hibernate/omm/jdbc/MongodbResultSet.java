package org.hibernate.omm.jdbc;

import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.Nullable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.bson.*;
import org.hibernate.omm.jdbc.adapter.ResultSetAdapter;
import org.hibernate.omm.jdbc.exception.BsonNullValueSQLException;
import org.hibernate.omm.jdbc.exception.ResultSetClosedSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;

public class MongodbResultSet extends ResultSetAdapter {

  private final MongoDatabase mongoDatabase;
  private Iterator<BsonDocument> currentBatchIterator;
  private Long currentCursorId;
  private final String collection;
  private final Integer batchSize;
  private BsonDocument currentDocument;
  private List<String> currentDocumentKeys = Collections.emptyList();
  private BsonValue lastRead;

  private volatile boolean closed;

  public MongodbResultSet(
      MongoDatabase mongoDatabase,
      String collection,
      Document findCommandResult,
      @Nullable Integer batchSize) {
    this.mongoDatabase = mongoDatabase;
    this.collection = collection;
    Document cursor = findCommandResult.get("cursor", Document.class);
    this.currentBatchIterator = cursor.getList("firstBatch", BsonDocument.class).iterator();
    this.currentCursorId = cursor.getLong("id");
    this.batchSize = batchSize;
  }

  @Override
  public boolean next() throws SimulatedSQLException {
    throwExceptionIfClosed();
    if (currentBatchIterator.hasNext() || getMore()) {
      currentDocument = currentBatchIterator.next();
      currentDocumentKeys = new ArrayList<>(currentDocument.keySet());
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public boolean wasNull() throws SimulatedSQLException {
    throwExceptionIfClosed();
    return lastRead != null && lastRead.isNull();
  }

  @Override
  public String getString(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonString bsonValue = currentDocument.getString(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    return bsonValue.isNull() ? null : bsonValue.getValue();
  }

  @Override
  public boolean getBoolean(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonBoolean bsonValue = currentDocument.getBoolean(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    if (bsonValue.isNull()) {
      throw new BsonNullValueSQLException();
    }
    return bsonValue.getValue();
  }

  @Override
  public byte getByte(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonNumber bsonValue = currentDocument.getNumber(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    if (bsonValue.isNull()) {
      throw new BsonNullValueSQLException();
    }
    return (byte) bsonValue.intValue();
  }

  @Override
  public short getShort(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonNumber bsonValue = currentDocument.getNumber(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    if (bsonValue.isNull()) {
      throw new BsonNullValueSQLException();
    }
    return (short) bsonValue.intValue();
  }

  @Override
  public int getInt(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonNumber bsonValue = currentDocument.getNumber(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    if (bsonValue.isNull()) {
      throw new BsonNullValueSQLException();
    }
    return bsonValue.intValue();
  }

  @Override
  public long getLong(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonNumber bsonValue = currentDocument.getNumber(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    if (bsonValue.isNull()) {
      throw new BsonNullValueSQLException();
    }
    return bsonValue.longValue();
  }

  @Override
  public float getFloat(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonNumber bsonValue = currentDocument.getNumber(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    if (bsonValue.isNull()) {
      throw new BsonNullValueSQLException();
    }
    return (float) bsonValue.doubleValue();
  }

  @Override
  public double getDouble(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonNumber bsonValue = currentDocument.getNumber(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    if (bsonValue.isNull()) {
      throw new BsonNullValueSQLException();
    }
    return bsonValue.doubleValue();
  }

  @Override
  public byte[] getBytes(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonBinary bsonValue = currentDocument.getBinary(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    return bsonValue.isNull() ? null : bsonValue.getData();
  }

  @Override
  public Date getDate(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonDateTime bsonValue = currentDocument.getDateTime(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    return bsonValue.isNull() ? null : new Date(bsonValue.getValue());
  }

  @Override
  public Time getTime(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonDateTime bsonValue = currentDocument.getDateTime(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    return bsonValue.isNull() ? null : new Time(bsonValue.getValue());
  }

  @Override
  public Timestamp getTimestamp(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonDateTime bsonValue = currentDocument.getDateTime(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    return bsonValue.isNull() ? null : new Timestamp(bsonValue.getValue());
  }

  @Override
  public BigDecimal getBigDecimal(int columnIndex) throws SimulatedSQLException {
    throwExceptionIfClosed();
    BsonDecimal128 bsonValue = currentDocument.getDecimal128(currentDocumentKeys.get(columnIndex));
    lastRead = bsonValue;
    return bsonValue.isNull() ? null : bsonValue.getValue().bigDecimalValue();
  }

  private boolean getMore() throws SimulatedSQLException {
    throwExceptionIfClosed();
    Document command =
        new Document().append("getMore", currentCursorId).append("collection", collection);
    if (batchSize != null) {
      command.append("batchSize", batchSize);
    }
    Document result = mongoDatabase.runCommand(command);
    List<BsonDocument> nextBatch =
        result.get("cursor", Document.class).getList("nextBatch", BsonDocument.class);
    currentBatchIterator = nextBatch.iterator();
    if (currentBatchIterator.hasNext()) {
      currentCursorId = result.get("cursor", Document.class).getLong("id");
      return true;
    } else {
      return false;
    }
  }

  private void throwExceptionIfClosed() throws ResultSetClosedSQLException {
    if (closed) {
      throw new ResultSetClosedSQLException();
    }
  }
}
