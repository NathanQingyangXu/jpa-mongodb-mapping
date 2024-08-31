/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.omm.jdbc;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.Nullable;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.types.ObjectId;
import org.hibernate.omm.cfg.MongoReadWriteOptionsStrategy;
import org.hibernate.omm.exception.NotYetImplementedException;
import org.hibernate.omm.jdbc.adapter.PreparedStatementAdapter;
import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;
import org.hibernate.omm.service.CommandRecorder;
import org.hibernate.omm.util.TypeUtil;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulate JDBC's {@link java.sql.PreparedStatement} to create a virtual MongoDB JDBC driver
 * on top of MongoDB Java driver.
 *
 * @author Nathan Xu
 * @apiNote v2 extended JSON format is observed when generating command JSON string.
 * @see <a href="https://www.mongodb.com/docs/manual/reference/mongodb-extended-json/">MongoDB Extended JSON (v2)</a>
 * @since 1.0.0
 */
public class MongoPreparedStatement extends MongoStatement
        implements PreparedStatementAdapter {

    private final String parameterizedCommandJson;
    private final Map<Integer, BsonValue> parameters;

    public MongoPreparedStatement(
            final MongoDatabase mongoDatabase,
            final ClientSession clientSession,
            final Connection connection,
            @Nullable final MongoReadWriteOptionsStrategy mongoReadWriteOptionsStrategy,
            @Nullable final CommandRecorder commandRecorder,
            final String parameterizedCommandJson) {
        super(mongoDatabase, clientSession, connection, mongoReadWriteOptionsStrategy, commandRecorder);
        this.parameterizedCommandJson = parameterizedCommandJson;
        this.parameters = new HashMap<>();
    }

    @Override
    public ResultSet executeQuery() throws SimulatedSQLException {
        return executeQuery(parameterizedCommandJson);
    }

    @Override
    public int executeUpdate() throws SimulatedSQLException {
        return executeUpdate(parameterizedCommandJson);
    }

    @Override
    public boolean execute() throws SimulatedSQLException {
        return execute(parameterizedCommandJson);
    }

    @Override
    public void addBatch() {
        // no-op
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType) {
        parameters.put(parameterIndex, TypeUtil.wrap(null));
    }

    @Override
    public void setBoolean(final int parameterIndex, final boolean x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setByte(final int parameterIndex, final byte x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setShort(final int parameterIndex, final short x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setInt(final int parameterIndex, final int x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setLong(final int parameterIndex, final long x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setFloat(final int parameterIndex, final float x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setDouble(final int parameterIndex, final double x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setBigDecimal(final int parameterIndex, final BigDecimal x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setString(final int parameterIndex, final String x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setBytes(final int parameterIndex, final byte[] x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setDate(final int parameterIndex, final Date x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setTime(final int parameterIndex, final Time x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setTimestamp(final int parameterIndex, final Timestamp x) {
        parameters.put(parameterIndex, TypeUtil.wrap(x));
    }

    @Override
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType)
            throws SimulatedSQLException {
        switch (targetSqlType) {
            case Types.STRUCT:
                parameters.put(parameterIndex, (BsonDocument) x);
                break;
            case 3_000:
                parameters.put(parameterIndex, new BsonObjectId((ObjectId) x));
                break;
            default:
                throw new NotSupportedSQLException("unknown SQL type: " + targetSqlType);
        }
    }

    @Override
    public void setArray(final int parameterIndex, final Array x) throws SQLException {
        parameters.put(parameterIndex, TypeUtil.wrap(x.getArray()));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // the following methods are used in Hibernate internally and likely we need to override
    // but for now we left them here for placeholders

    @Override
    public void setObject(final int parameterIndex, @Nullable final Object x) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setBlob(final int parameterIndex, @Nullable final Blob x) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setClob(final int parameterIndex, @Nullable final Clob x) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setDate(final int parameterIndex, @Nullable final Date x, @Nullable final Calendar cal) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setTime(final int parameterIndex, @Nullable final Time x, @Nullable final Calendar cal) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setTimestamp(final int parameterIndex, @Nullable final Timestamp x, @Nullable final Calendar cal) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType, final String typeName) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setRowId(final int parameterIndex, @Nullable final RowId x) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setNString(final int parameterIndex, @Nullable final String value) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setNCharacterStream(final int parameterIndex, @Nullable final Reader value, final long length) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setNClob(final int parameterIndex, @Nullable final NClob value) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setClob(final int parameterIndex, @Nullable final Reader reader, final long length) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setBlob(final int parameterIndex, @Nullable final InputStream inputStream, final long length) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setNClob(final int parameterIndex, @Nullable final Reader reader, final long length) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setSQLXML(final int parameterIndex, @Nullable final SQLXML xmlObject) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setBinaryStream(final int parameterIndex, @Nullable final InputStream x, final long length) {
        throw new NotYetImplementedException();
    }

    @Override
    public void setCharacterStream(final int parameterIndex, @Nullable final Reader reader, final long length) {
        throw new NotYetImplementedException();
    }

    @Override
    protected void replaceParameterMarkers(final BsonDocument command) {
        recursivelyReplaceParameterMarkers(command, 1);
    }

    private int recursivelyReplaceParameterMarkers(BsonDocument command, int curParameterIndex) {
        for (Map.Entry<String, BsonValue> entry : command.entrySet()) {
            if (entry.getValue().getBsonType() == BsonType.UNDEFINED) {
                entry.setValue(parameters.get(curParameterIndex));
                curParameterIndex++;
            } else if (entry.getValue().isDocument()) {
                curParameterIndex = recursivelyReplaceParameterMarkers(entry.getValue().asDocument(), curParameterIndex);
            } else if (entry.getValue().isArray()) {
                curParameterIndex = recursivelyReplaceParameterMarkers(entry.getValue().asArray(), curParameterIndex);
            }
        }
        return curParameterIndex;
    }

    private int recursivelyReplaceParameterMarkers(BsonArray array, int curParameterIndex) {
        for (int i = 0; i < array.size(); i++) {
            BsonValue value = array.get(i);
            if (value.getBsonType() == BsonType.UNDEFINED) {
                array.set(i, parameters.get(curParameterIndex));
                curParameterIndex++;
            } else if (value.isDocument()) {
                curParameterIndex = recursivelyReplaceParameterMarkers(value.asDocument(), curParameterIndex);
            } else if (value.isArray()) {
                curParameterIndex = recursivelyReplaceParameterMarkers(value.asArray(), curParameterIndex);
            }
        }
        return curParameterIndex;
    }
}
