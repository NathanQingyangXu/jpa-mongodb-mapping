package org.hibernate.omm.jdbc;

import com.mongodb.assertions.Assertions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.hibernate.omm.jdbc.adapter.StatementAdapter;
import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;
import org.hibernate.omm.jdbc.exception.StatementClosedSQLException;
import org.hibernate.omm.service.CommandRecorder;
import org.hibernate.omm.translate.translator.MQLTranslator;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoStatement implements StatementAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MongoStatement.class);

    @Nullable public final CommandRecorder commandRecorder;

    private final MongoDatabase mongoDatabase;
    private final ClientSession clientSession;
    private final Connection connection;

    private boolean closed;

    public MongoStatement(
            final MongoDatabase mongoDatabase,
            final ClientSession clientSession,
            final Connection connection,
            final @Nullable CommandRecorder commandRecorder) {
        Assertions.notNull("mongoDatabase", mongoDatabase);
        Assertions.notNull("clientSession", clientSession);
        Assertions.notNull("connection", connection);
        this.mongoDatabase = mongoDatabase;
        this.clientSession = clientSession;
        this.connection = connection;
        this.commandRecorder = commandRecorder;
    }

    @Override
    public ResultSet executeQuery(final String sql) throws SimulatedSQLException {
        Assertions.notNull("sql", sql);
        throwExceptionIfClosed();

        var command = BsonDocument.parse(sql);
        replaceParameterMarkers(command);
        logAndRecordCommand(command);

        var collection =
                mongoDatabase.getCollection(command.getString("aggregate").getValue(), BsonDocument.class);
        var pipeline =
                command.getArray("pipeline").stream().map(BsonValue::asDocument).toList();
        var cursor = collection.aggregate(clientSession, pipeline).cursor();
        var fieldNames = getFieldNamesFromProjectDocument(
                pipeline.get(pipeline.size() - 1).asDocument().getDocument("$project"));
        return new MongoResultSet(cursor, fieldNames);
    }

    /**
     * Get explicitly ordered field name list from $project document
     *
     * @param projectDocument the $project document
     * @return ordered field name list
     * @see MQLTranslator#visitSelectClause(SelectClause)
     */
    private List<String> getFieldNamesFromProjectDocument(final BsonDocument projectDocument) {
        // we rely on $project field renaming to ensure order
        // but we also skip '_id' explicitly (e.g. _id: 0)
        var fieldNames = new ArrayList<String>(projectDocument.size());
        for (Map.Entry<String, BsonValue> entry : projectDocument.entrySet()) {
            var value = entry.getValue();
            boolean skip = value.isNumber() && value.asNumber().intValue() == 0
                    || value.isBoolean() && value.asBoolean().equals(BsonBoolean.FALSE);
            if (!skip) {
                fieldNames.add(entry.getKey());
            }
        }
        return fieldNames;
    }

    @Override
    public int executeUpdate(final String sql) throws SimulatedSQLException {
        Assertions.notNull("sql", sql);
        throwExceptionIfClosed();
        BsonDocument command = BsonDocument.parse(sql);

        replaceParameterMarkers(command);
        logAndRecordCommand(command);

        String commandName = command.getFirstKey();
        MongoCollection<BsonDocument> collection =
                mongoDatabase.getCollection(command.getString(commandName).getValue(), BsonDocument.class);
        switch (commandName) {
            case "insert":
                BsonDocument document = command.getArray("documents").get(0).asDocument();
                collection.insertOne(clientSession, document);
                return 1;
            case "update":
                BsonDocument updateDocument = command.getArray("updates").get(0).asDocument();
                UpdateResult updateResult = !updateDocument.getBoolean("multi").getValue()
                        ? collection.updateOne(
                                clientSession, updateDocument.getDocument("q"), updateDocument.getDocument("u"))
                        : collection.updateMany(
                                clientSession, updateDocument.getDocument("q"), updateDocument.getDocument("u"));
                return (int) updateResult.getModifiedCount();
            case "delete":
                BsonDocument deleteDocument = command.getArray("deletes").get(0).asDocument();
                DeleteResult deleteResult = deleteDocument.getNumber("limit").intValue() == 1
                        ? collection.deleteOne(clientSession, deleteDocument.getDocument("q"))
                        : collection.deleteMany(clientSession, deleteDocument.getDocument("q"));
                return (int) deleteResult.getDeletedCount();
            default:
                throw new NotSupportedSQLException("unknown command: " + commandName);
        }
    }

    protected void replaceParameterMarkers(final BsonDocument command) {}

    private void logAndRecordCommand(final BsonDocument command) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(command.toJson());
        }
        if (commandRecorder != null) {
            commandRecorder.record(command);
        }
    }

    @Override
    public boolean execute(final String sql) throws SimulatedSQLException {
        Assertions.notNull("sql", sql);
        throwExceptionIfClosed();

        var command = BsonDocument.parse(sql);
        replaceParameterMarkers(command);
        logAndRecordCommand(command);

        var commandResult = mongoDatabase.runCommand(command);
        return commandResult.getDouble("ok") == 1.0;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public void cancel() throws SimulatedSQLException {
        throwExceptionIfClosed();
        throw new NotSupportedSQLException();
    }

    @Override
    public SQLWarning getWarnings() throws SimulatedSQLException {
        throwExceptionIfClosed();
        return null;
    }

    @Override
    public void clearWarnings() throws SimulatedSQLException {
        throwExceptionIfClosed();
    }

    @Override
    @Nullable public ResultSet getResultSet() throws SimulatedSQLException {
        throwExceptionIfClosed();
        throw new NotSupportedSQLException();
    }

    @Override
    public int getUpdateCount() {
        // unclear what this is, throw for now.
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getMoreResults() throws SimulatedSQLException {
        throwExceptionIfClosed();
        throw new NotSupportedSQLException();
    }

    @Override
    public void setFetchSize(final int rows) throws SimulatedSQLException {
        throwExceptionIfClosed();
        throw new NotSupportedSQLException();
    }

    @Override
    public int getFetchSize() throws SimulatedSQLException {
        throwExceptionIfClosed();
        throw new NotSupportedSQLException();
    }

    @Override
    public void addBatch(final String sql) throws SimulatedSQLException {
        throwExceptionIfClosed();
        throw new NotSupportedSQLException();
    }

    @Override
    public void clearBatch() throws SimulatedSQLException {
        throwExceptionIfClosed();
        throw new NotSupportedSQLException();
    }

    @Override
    public int[] executeBatch() throws SimulatedSQLException {
        throwExceptionIfClosed();
        throw new NotSupportedSQLException();
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SimulatedSQLException {
        throwExceptionIfClosed();
        throw new NotSupportedSQLException();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    private void throwExceptionIfClosed() throws StatementClosedSQLException {
        if (closed) {
            throw new StatementClosedSQLException();
        }
    }
}
