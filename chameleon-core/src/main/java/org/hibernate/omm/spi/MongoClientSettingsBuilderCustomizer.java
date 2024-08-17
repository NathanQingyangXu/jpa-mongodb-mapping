package org.hibernate.omm.spi;

import com.mongodb.MongoClientSettings;

@FunctionalInterface
public interface MongoClientSettingsBuilderCustomizer {
    void customize(MongoClientSettings.Builder builder);
}
