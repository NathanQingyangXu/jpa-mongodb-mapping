package org.hibernate.omm.jdbc;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.hibernate.omm.jdbc.adapter.ArrayAdapter;
import org.hibernate.omm.jdbc.adapter.ConnectionAdapter;
import org.hibernate.omm.jdbc.adapter.DatabaseMetaDataAdapter;

import java.sql.Array;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;

public class MongodbConnection extends ConnectionAdapter {

    private final ClientSession session;
    private final MongoDatabase mongoDatabase;

    private boolean autoCommit = true;
    private boolean closed;

    private SQLWarning sqlWarning;

    public MongodbConnection(ClientSession session, MongoDatabase mongoDatabase) {
        this.session = session;
        this.mongoDatabase = mongoDatabase;
    }

    @Override
    public Statement createStatement() {
        return new MongodbStatement(mongoDatabase, this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) {
        return new MongodbPreparedStatement(mongoDatabase, this, sql);
    }

    @Override
    public DatabaseMetaData getMetaData() {
        Document result = mongoDatabase.runCommand(new Document("buildinfo", 1));
        String version = result.getString("version");
        List<Integer> versionArray = result.getList("versionArray", Integer.class);
        return new DatabaseMetaDataAdapter() {

            @Override
            public String getDatabaseProductVersion() {
                return version;
            }

            @Override
            public int getDatabaseMajorVersion() {
                return versionArray.get(0);
            }

            @Override
            public int getDatabaseMinorVersion() {
                return versionArray.get(1);
            }

        };
    }

    @Override
    public boolean getAutoCommit() {
        return this.autoCommit;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
        if (!autoCommit && !session.hasActiveTransaction()) {
            session.startTransaction();
        }
    }

    @Override
    public void commit() {
        session.commitTransaction();
        session.startTransaction();
    }

    @Override
    public void rollback() {
        session.abortTransaction();
        session.startTransaction();
    }

    @Override
    public void close() {
        this.session.close();
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
    public SQLWarning getWarnings() {
        return sqlWarning;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) {
        return new ArrayAdapter() {
            @Override
            public int getBaseType() {
                return 0;
            }

            @Override
            public Object getArray() {
                return elements;
            }
        };
    }
}
