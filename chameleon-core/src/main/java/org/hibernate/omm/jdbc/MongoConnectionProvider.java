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

import com.mongodb.ClientSessionOptions;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;
import org.bson.assertions.Assertions;
import org.bson.codecs.configuration.CodecRegistry;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.omm.cfg.MongoAvailableSettings;
import org.hibernate.omm.cfg.MongoReadWriteOptionsStrategy;
import org.hibernate.omm.exception.MongoConfigInvalidException;
import org.hibernate.omm.exception.MongoConfigMissingException;
import org.hibernate.omm.service.CommandRecorder;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoConnectionProvider implements ConnectionProvider, Configurable, Startable, Stoppable, ServiceRegistryAwareService {

    private @MonotonicNonNull String mongoConnectionURL;
    private @MonotonicNonNull String mongoDatabaseName;
    private @MonotonicNonNull ServiceRegistryImplementor serviceRegistry;

    private @MonotonicNonNull MongoDatabase mongoDatabase;
    private @MonotonicNonNull MongoClient mongoClient;

    private @MonotonicNonNull MongoReadWriteOptionsStrategy mongoReadWriteOptionsStrategy;

    @Override
    public void configure(final Map<String, Object> configurationValues) {
        final var missingMandatoryConfigurations =
                Arrays.stream(MongoAvailableSettings.values())
                        .filter(MongoAvailableSettings::isMandatory)
                        .map(MongoAvailableSettings::getConfiguration)
                        .filter(Predicate.not(configurationValues::containsKey))
                        .toList();
        if (!missingMandatoryConfigurations.isEmpty()) {
            throw new MongoConfigMissingException(missingMandatoryConfigurations);
        }

        mongoConnectionURL =
                (String) configurationValues.get(MongoAvailableSettings.MONGODB_CONNECTION_URL.getConfiguration());
        Assertions.assertNotNull(mongoConnectionURL);

        mongoDatabaseName =
                (String) configurationValues.get(MongoAvailableSettings.MONGODB_DATABASE.getConfiguration());
        Assertions.assertNotNull(mongoDatabaseName);

        final var mongoTransactionOptionsStrategyProviderClass =
                (String) configurationValues.get(MongoAvailableSettings.MONGODB_READ_WRITE_OPTIONS_STRATEGY_CLASS.getConfiguration());

        if (mongoTransactionOptionsStrategyProviderClass != null) {
            try {
                mongoReadWriteOptionsStrategy =
                        (MongoReadWriteOptionsStrategy) Class.forName(mongoTransactionOptionsStrategyProviderClass).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new MongoConfigInvalidException(MongoAvailableSettings.MONGODB_READ_WRITE_OPTIONS_STRATEGY_CLASS.getConfiguration()
                        , "Unable to instantiate Class: " + mongoTransactionOptionsStrategyProviderClass, e);
            }
        }

    }

    @Override
    public void start() {
        mongoClient = buildMongoClient(mongoConnectionURL);
        mongoDatabase = buildMongoDatabase(mongoClient, mongoDatabaseName);
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

        final var clientSession = buildClientSession(mongoClient);
        final var commandRecorder = serviceRegistry.getService(CommandRecorder.class);
        return new MongoConnection(mongoDatabase, clientSession, mongoReadWriteOptionsStrategy, commandRecorder);
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

    @Nullable
    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    @Override
    public void injectServices(@NonNull final ServiceRegistryImplementor serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    private MongoClient buildMongoClient(final String mongodbConnectionURL) {
        final var connectionString = new ConnectionString(Objects.requireNonNull(mongodbConnectionURL));
        CodecRegistry codecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry()
        );
        final var clientSettingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(codecRegistry);
        if (mongoReadWriteOptionsStrategy != null) {

            final var clientReadPreference = mongoReadWriteOptionsStrategy.clientReadPreference();
            if (clientReadPreference != null) {
                clientSettingsBuilder.readPreference(clientReadPreference);
            }

            final var clientReadConcern = mongoReadWriteOptionsStrategy.clientReadConcern();
            if (clientReadConcern != null) {
                clientSettingsBuilder.readConcern(clientReadConcern);
            }

            final var clientWriteConcern = mongoReadWriteOptionsStrategy.clientWriteConcern();
            if (clientWriteConcern != null) {
                clientSettingsBuilder.writeConcern(clientWriteConcern);
            }

        }
        return MongoClients.create(clientSettingsBuilder.build());
    }

    private MongoDatabase buildMongoDatabase(final MongoClient mongoClient, final String mongodbDatabaseName) {
        var database = mongoClient.getDatabase(Objects.requireNonNull(mongodbDatabaseName));
        if (mongoReadWriteOptionsStrategy != null) {
            final var readPreference = mongoReadWriteOptionsStrategy.databaseReadPreference();
            if (readPreference != null) {
                database = database.withReadPreference(readPreference);
            }
            final var readConcern = mongoReadWriteOptionsStrategy.databaseReadConcern();
            if (readConcern != null) {
                database = database.withReadConcern(readConcern);
            }
            final var writeConcern = mongoReadWriteOptionsStrategy.databaseWriteConcern();
            if (writeConcern != null) {
                database = database.withWriteConcern(writeConcern);
            }
        }
        return database;
    }

    private ClientSession buildClientSession(final MongoClient mongoClient) {
        final var sessionOptionsBuilder = ClientSessionOptions.builder();
        if (mongoReadWriteOptionsStrategy != null) {
            final var defaultTransactionOptions = mongoReadWriteOptionsStrategy.sessionDefaultTransactionOptions(mongoClient);
            if (defaultTransactionOptions != null) {
                sessionOptionsBuilder.defaultTransactionOptions(defaultTransactionOptions);
            }
            return mongoClient.startSession(sessionOptionsBuilder.build());
        }
        else {
            return mongoClient.startSession();
        }
    }

}
