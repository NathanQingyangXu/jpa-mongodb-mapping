package org.example;

import org.hibernate.omm.spi.MongoClientSettingsBuilderCustomizer;

public class DummyMongoClientSettingsBuilderCustomizer implements MongoClientSettingsBuilderCustomizer {
    @Override
    public void customize(final com.mongodb.MongoClientSettings.Builder builder) {
        System.out.println("Hello world!");
    }
}
