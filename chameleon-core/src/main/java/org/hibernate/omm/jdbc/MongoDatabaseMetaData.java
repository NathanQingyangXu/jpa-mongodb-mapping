package org.hibernate.omm.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.hibernate.omm.jdbc.adapter.DatabaseMetaDataAdapter;

/**
 * Virtual Mongo JDBC Driver's minimal metadata feature set to ensure no exception is thrown during JDBC Environment
 * building.
 * <p/>
 * Needs to re-evaluate whether the provided metadata is correct. For the time being, they are
 * provided as dummy overriding to avoid awkward exception during {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment}
 * building phase.
 *
 * @author Nathan Xu
 * @since 1.0.0
 * @see org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl
 */
public class MongoDatabaseMetaData implements DatabaseMetaDataAdapter {
    public static final String MONGO_DATABASE_PRODUCT_NAME = "MongoDB";

    private final String version;
    private final int majorVersion;
    private final int minorVersion;
    private final MongoConnection mongoConnection;

    public MongoDatabaseMetaData(
            final String version,
            final int majorVersion,
            final int minorVersion,
            final MongoConnection mongoConnection) {
        this.version = version;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public String getDatabaseProductName() {
        return MONGO_DATABASE_PRODUCT_NAME;
    }

    @Override
    public String getDatabaseProductVersion() {
        return version;
    }

    @Override
    public int getDatabaseMajorVersion() {
        return majorVersion;
    }

    @Override
    public int getDatabaseMinorVersion() {
        return minorVersion;
    }

    @Override
    public Connection getConnection() {
        return mongoConnection;
    }

    @Override
    public String getDriverName() {
        return "Chameleon JDBC Driver";
    }

    @Override
    public int getDriverMajorVersion() {
        return -1;
    }

    @Override
    public int getDriverMinorVersion() {
        return -1;
    }

    @Override
    public int getJDBCMajorVersion() {
        return 4;
    }

    @Override
    public int getJDBCMinorVersion() {
        return 3;
    }

    @Override
    public String getSQLKeywords() {
        return "";
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() {
        return false;
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() {
        return false;
    }

    @Override
    public boolean storesLowerCaseIdentifiers() {
        return false;
    }

    @Override
    public boolean storesUpperCaseIdentifiers() {
        return false;
    }

    @Override
    public boolean storesMixedCaseIdentifiers() {
        return true;
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() {
        return false;
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() {
        return false;
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() {
        return true;
    }

    @Override
    public boolean supportsNamedParameters() {
        return true;
    }

    @Override
    public boolean supportsResultSetType(int type) {
        return false;
    }

    @Override
    public boolean supportsGetGeneratedKeys() {
        return true;
    }

    @Override
    public boolean supportsBatchUpdates() {
        return true;
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() {
        return true;
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() {
        return false;
    }

    @Override
    public int getSQLStateType() {
        return DatabaseMetaData.sqlStateSQL;
    }

    @Override
    public String getCatalogSeparator() {
        return "\n";
    }

    @Override
    public boolean isCatalogAtStart() {
        return true;
    }
}
