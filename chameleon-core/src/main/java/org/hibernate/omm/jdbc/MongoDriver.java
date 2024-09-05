package org.hibernate.omm.jdbc;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ServerMonitoringMode;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.hibernate.omm.jdbc.adapter.DriverAdapter;
import org.hibernate.omm.service.CommandRecorder;

import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDriver implements DriverAdapter {

    public static volatile @MonotonicNonNull MongoDatabase mongoDatabase;
    public static MongoClient mongoClient;

    public static CommandRecorder commandRecorder;


    public static MongoDatabase getMongoDatabase(String url) {
        initializeMongoDatabase(url);
        return mongoDatabase;
    }

    @Override
    public Connection connect(final String url, final Properties info) {
        if (mongoDatabase == null) {
            synchronized (this) {
                if (mongoDatabase == null) {
                    initializeMongoDatabase(url);
                }
            }
        }
        final var clientSession = mongoClient.startSession();
        return new MongoConnection(mongoDatabase, clientSession);
    }

    @Override
    public boolean acceptsURL(final String url) {
        return url != null && url.startsWith("mongodb+");
    }

    private static void initializeMongoDatabase(final String url) {
        final var mongoDatabaseName = requireNonNull(extractDatabaseFromConnectionString(url));
        final var connectionString = new ConnectionString(url);
        final var codecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry()
        );

        final var clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(codecRegistry)
                .applyToServerSettings(builder -> builder.serverMonitoringMode(ServerMonitoringMode.STREAM))
                .build();
        mongoClient = MongoClients.create(clientSettings);
        mongoDatabase = mongoClient.getDatabase(mongoDatabaseName);
    }

    private static String extractDatabaseFromConnectionString(final String connectionString) {
        int startIndex = connectionString.lastIndexOf('/');
        int endIndex = connectionString.indexOf(startIndex + 1, '?');
        if (endIndex < 0) {
            return connectionString.substring(startIndex + 1);
        }
        else {
            return connectionString.substring(startIndex, endIndex);
        }
    }

}
