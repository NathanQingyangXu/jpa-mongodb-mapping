package org.hibernate.omm.example;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.omm.service.MongoClientSettingsCustomizer;

public class ProgrammaticClientSettingsCustomize {

  public static void main(String[] args) {

    final var cfg = new Configuration();
    cfg.setProperty(AvailableSettings.JAKARTA_JDBC_URL, "mongodb://127.0.0.1:27017/test");

    // add your entity classes

    final var standardServiceRegistryBuilder = cfg.getStandardServiceRegistryBuilder();

    MongoClientSettingsCustomizer customizer = builder -> {
      System.out.println("Do whatever you want");
    };

    standardServiceRegistryBuilder.addService(MongoClientSettingsCustomizer.class, customizer);

    try (var sessionFactory = (SessionFactoryImplementor) cfg.buildSessionFactory()) {

      sessionFactory.inStatelessSession(statelessSession -> {
        System.out.println("Hello World!");
      });
    }

  }
}
