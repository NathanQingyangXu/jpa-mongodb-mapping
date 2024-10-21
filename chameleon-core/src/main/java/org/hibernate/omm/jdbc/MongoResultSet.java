package org.hibernate.omm.jdbc;

import static java.util.Objects.requireNonNull;

import com.mongodb.assertions.Assertions;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.Nullable;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.BsonValue;
import org.hibernate.omm.jdbc.adapter.ArrayAdapter;
import org.hibernate.omm.jdbc.adapter.ResultSetAdapter;
import org.hibernate.omm.jdbc.adapter.ResultSetMetaDataAdapter;
import org.hibernate.omm.jdbc.exception.CurrentDocumentNullSQLException;
import org.hibernate.omm.jdbc.exception.ResultSetClosedSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;
import org.hibernate.omm.util.CollectionUtil;
import org.hibernate.omm.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoResultSet implements ResultSetAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoResultSet.class);

    private final MongoCursor<BsonDocument> cursor;

    private final List<String> fieldNames;

    @Nullable private BsonDocument currentDocument;

    private BsonValue lastRead;

    private boolean closed;

    public MongoResultSet(final MongoCursor<BsonDocument> cursor, final List<String> fieldNames) {
        Assertions.notNull("cursor", cursor);
        Assertions.notNull("fieldNames", fieldNames);
        this.cursor = cursor;
        this.fieldNames = fieldNames;
    }

    @Override
    public boolean next() throws SimulatedSQLException {
        if (closed) {
            throw new ResultSetClosedSQLException();
        }
        if (cursor.hasNext()) {
            currentDocument = cursor.next().toBsonDocument();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(requireNonNull(currentDocument).toJson());
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        closed = true;
        cursor.close();
    }

    @Override
    public boolean wasNull() throws SimulatedSQLException {
        if (closed) {
            throw new ResultSetClosedSQLException();
        }
        return requireNonNull(lastRead).isNull();
    }

    private BsonValue getBsonValue(int columnIndex) throws SimulatedSQLException {
        beforeAccessCurrentDocumentField();
        lastRead = requireNonNull(currentDocument).get(getKey(columnIndex), BsonNull.VALUE);
        return lastRead;
    }

    @Override
    @Nullable public String getString(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? null : bsonValue.asString().getValue();
    }

    @Override
    public boolean getBoolean(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return !bsonValue.isNull() && bsonValue.asBoolean().getValue();
    }

    @Override
    public byte getByte(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return (byte) (bsonValue.isNull() ? 0 : bsonValue.asNumber().intValue());
    }

    @Override
    public short getShort(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return (short) (bsonValue.isNull() ? 0 : bsonValue.asNumber().intValue());
    }

    @Override
    public int getInt(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? 0 : bsonValue.asNumber().intValue();
    }

    @Override
    public long getLong(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? 0 : bsonValue.asNumber().longValue();
    }

    @Override
    public float getFloat(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? 0 : (float) bsonValue.asNumber().doubleValue();
    }

    @Override
    public double getDouble(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? 0 : bsonValue.asNumber().doubleValue();
    }

    @Override
    @Nullable public byte[] getBytes(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? null : bsonValue.asBinary().getData();
    }

    @Override
    @Nullable public Date getDate(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? null : new Date(bsonValue.asDateTime().getValue());
    }

    @Override
    @Nullable public Time getTime(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? null : new Time(bsonValue.asDateTime().getValue());
    }

    @Override
    @Nullable public Timestamp getTimestamp(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? null : new Timestamp(bsonValue.asDateTime().getValue());
    }

    @Override
    @Nullable public BigDecimal getBigDecimal(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull()
                ? null
                : bsonValue.asDecimal128().decimal128Value().bigDecimalValue();
    }

    @Override
    public Array getArray(final int columnIndex) throws SimulatedSQLException {
        BsonValue bsonValue = getBsonValue(columnIndex);
        if (bsonValue.isNull()) {
            return null;
        }
        List<BsonValue> bsonValues = bsonValue.asArray();
        return new ArrayAdapter() {

            @Override
            public int getBaseType() {
                return CollectionUtil.isEmpty(bsonValues)
                        ? Types.NULL
                        : TypeUtil.getJdbcType(bsonValues.get(0).getBsonType());
            }

            @Override
            public Object getArray() {
                return bsonValues.stream().map(TypeUtil::unwrap).toArray();
            }
        };
    }

    @Override
    @Nullable public Object getObject(final int columnIndex, final Class type) throws SimulatedSQLException {
        Assertions.notNull("type", type);
        BsonValue bsonValue = getBsonValue(columnIndex);
        return bsonValue.isNull() ? null : type.cast(bsonValue);
    }

    @Override
    public ResultSetMetaData getMetaData() {
        return new ResultSetMetaDataAdapter() {

            @Override
            public int getColumnCount() throws SimulatedSQLException {
                return fieldNames.size();
            }

            @Override
            public String getColumnLabel(final int column) throws SimulatedSQLException {
                return fieldNames.get(column - 1);
            }
        };
    }

    @Override
    public int findColumn(final String columnLabel) {
        Assertions.notNull("columnLabel", columnLabel);
        return fieldNames.indexOf(columnLabel) + 1;
    }

    private String getKey(final int columnIndex) {
        return fieldNames.get(columnIndex - 1);
    }

    private void beforeAccessCurrentDocumentField()
            throws ResultSetClosedSQLException, CurrentDocumentNullSQLException {
        if (closed) {
            throw new ResultSetClosedSQLException();
        }
        if (currentDocument == null) {
            throw new CurrentDocumentNullSQLException();
        }
    }
}
