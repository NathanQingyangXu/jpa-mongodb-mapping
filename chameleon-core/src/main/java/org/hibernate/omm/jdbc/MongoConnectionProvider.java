package org.hibernate.omm.jdbc;

import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import org.bson.assertions.Assertions;
import org.bson.codecs.configuration.CodecRegistry;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.omm.service.CommandRecorder;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoConnectionProvider
        implements ConnectionProvider, Configurable, Startable, Stoppable, ServiceRegistryAwareService {

    private @MonotonicNonNull String mongoConnectionURL;
    private @MonotonicNonNull ServiceRegistryImplementor serviceRegistry;

    public @MonotonicNonNull MongoDatabase mongoDatabase;
    private @MonotonicNonNull MongoClient mongoClient;

    @Override
    public void configure(final Map<String, Object> configurationValues) {
        mongoConnectionURL =
                Assertions.assertNotNull((String) configurationValues.get(AvailableSettings.JAKARTA_JDBC_URL));
    }

    @Override
    public void start() {
        ConnectionString connectionString = new ConnectionString(mongoConnectionURL);
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry());

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(codecRegistry)
                .build();
        mongoClient = MongoClients.create(clientSettings);

        mongoDatabase = mongoClient.getDatabase(Assertions.assertNotNull(extractMongoDatabase(mongoConnectionURL)));
    }

    @Override
    public void stop() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public Connection getConnection() {
        if (mongoDatabase == null) {
            throw new IllegalStateException(
                    "mongoDatabase instance should have been configured during Configurable mechanism");
        }
        if (mongoClient == null) {
            throw new IllegalStateException(
                    "mongoClient instance should have been configured during Configurable mechanism");
        }
        if (serviceRegistry == null) {
            throw new IllegalStateException(
                    "serviceRegistry instance should have been configured during Configurable mechanism");
        }
        ClientSession clientSession = mongoClient.startSession();
        CommandRecorder commandRecorder = serviceRegistry.getService(CommandRecorder.class);
        return new MongoConnection(mongoDatabase, clientSession, commandRecorder);
    }

    @Override
    public void closeConnection(final Connection conn) throws SQLException {
        conn.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(@NonNull final Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(@NonNull final Class<T> unwrapType) {
        throw new UnknownUnwrapTypeException(unwrapType);
    }

    @Nullable public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    @Override
    public void injectServices(@NonNull final ServiceRegistryImplementor serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    private static String extractMongoDatabase(String jdbcURL) {
        final var connectionString = Objects.requireNonNull(jdbcURL);
        int startIndex = connectionString.lastIndexOf('/') + 1;
        int endIndex = connectionString.indexOf(startIndex, '?');
        if (endIndex < 0) {
            return connectionString.substring(startIndex);
        } else {
            return connectionString.substring(startIndex, endIndex);
        }
    }
}
