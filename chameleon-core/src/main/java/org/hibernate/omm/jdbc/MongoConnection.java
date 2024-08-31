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

import com.mongodb.assertions.Assertions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.Nullable;
import org.bson.Document;
import org.hibernate.omm.cfg.MongoReadWriteOptionsStrategy;
import org.hibernate.omm.exception.NotYetImplementedException;
import org.hibernate.omm.jdbc.adapter.ConnectionAdapter;
import org.hibernate.omm.jdbc.exception.CommandRunFailSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;
import org.hibernate.omm.service.CommandRecorder;
import org.hibernate.omm.type.array.MongoArray;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoConnection implements ConnectionAdapter {
    private static final String DB_VERSION_QUERY_FIELD_NAME = "buildinfo";
    private static final String VERSION_FIELD_NAME = "version";
    private static final String VERSION_ARRAY_FIELD_NAME = "versionArray";

    private final ClientSession clientSession;
    private final MongoDatabase mongoDatabase;

    @Nullable
    private final MongoReadWriteOptionsStrategy mongoReadWriteOptionsStrategy;

    @Nullable
    private final CommandRecorder commandRecorder;

    private boolean autoCommit;
    private boolean closed;

    @Nullable
    private SQLWarning sqlWarning;

    public MongoConnection(final MongoDatabase mongoDatabase, final ClientSession clientSession,
            @Nullable final MongoReadWriteOptionsStrategy mongoReadWriteOptionsStrategy, @Nullable final CommandRecorder commandRecorder) {
        Assertions.notNull("mongoDatabase", mongoDatabase);
        Assertions.notNull("clientSession", clientSession);
        this.clientSession = clientSession;
        this.mongoDatabase = mongoDatabase;
        this.mongoReadWriteOptionsStrategy = mongoReadWriteOptionsStrategy;
        this.commandRecorder = commandRecorder;
    }

    @Override
    public Statement createStatement() {
        return new MongoStatement(mongoDatabase, clientSession, this, mongoReadWriteOptionsStrategy, commandRecorder);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql) {
        return new MongoPreparedStatement(mongoDatabase, clientSession, this, mongoReadWriteOptionsStrategy, commandRecorder, sql);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) {
        return prepareStatement(sql);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SimulatedSQLException {
        Document result = mongoDatabase.runCommand(new Document(DB_VERSION_QUERY_FIELD_NAME, 1));
        if (result.getDouble("ok") != 1.0) {
            throw new CommandRunFailSQLException(result);
        }
        String version = result.getString(VERSION_FIELD_NAME);
        List<Integer> versionArray = result.getList(VERSION_ARRAY_FIELD_NAME, Integer.class);

        Assertions.assertTrue(versionArray.size() >= 2);

        return new MongoDatabaseMetaData(version, versionArray.get(0), versionArray.get(1), this);
    }

    @Override
    public CallableStatement prepareCall(final String sql) {
        throw new NotYetImplementedException();
    }

    @Override
    public boolean getAutoCommit() {
        return this.autoCommit;
    }

    @Override
    public void setAutoCommit(final boolean autoCommit) {
        if (!autoCommit) {
            startTransaction();
        }
        this.autoCommit = autoCommit;
    }

    @Override
    public void commit() {
        if (!autoCommit) {
            if (clientSession.hasActiveTransaction()) {
                clientSession.commitTransaction();
            }
        }
    }

    @Override
    public void rollback() {
        if (!autoCommit) {
            clientSession.abortTransaction();
        }
    }

    @Override
    public void close() {
        this.clientSession.close();
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void clearWarnings() {
        sqlWarning = null;
    }

    @Override
    @Nullable
    public SQLWarning getWarnings() {
        return sqlWarning;
    }

    @Override
    public String getCatalog() {
        return "N/A";
    }

    @Override
    public String getSchema() {
        return "N/A";
    }

    @Override
    public Array createArrayOf(final String typeName, final Object[] elements) {
        return new MongoArray(typeName, elements);
    }

    private void startTransaction() {
        if (mongoReadWriteOptionsStrategy != null && mongoReadWriteOptionsStrategy.transactionOptions(this.clientSession) != null) {
            final var transactionOptions = mongoReadWriteOptionsStrategy.transactionOptions(this.clientSession);
            if (transactionOptions != null) {
                this.clientSession.startTransaction(transactionOptions);
                return;
            }
        }
        this.clientSession.startTransaction();
    }
}
