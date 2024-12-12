package org.hibernate.omm;

import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SessionFactory;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.hibernate.omm.service.MongoSessionFactory;
import org.junit.jupiter.api.Test;

@MongoIntegrationTest
class ReadWriteConcernTests {

  @SessionFactoryInjected
  SessionFactory sessionFactory;

  @Test
  void test_client_level_concerns() {
    MongoSessionFactory mongoSessionFactory = (MongoSessionFactory) sessionFactory;
    mongoSessionFactory.setReadConcern(ReadConcern.MAJORITY);
    mongoSessionFactory.setWriteConcern(WriteConcern.ACKNOWLEDGED);

    mongoSessionFactory.inTransaction(session -> {
      System.out.println("Hello World");
    });
  }

  @Test
  void test_client_session_level_concerns() {
    MongoSessionFactory mongoSessionFactory = (MongoSessionFactory) sessionFactory;

    mongoSessionFactory.inSession(ReadConcern.MAJORITY, WriteConcern.ACKNOWLEDGED, session -> {
      System.out.println("Hello World");
    });
  }

  @Test
  void test_transaction_level_concerns() {
    MongoSessionFactory mongoSessionFactory = (MongoSessionFactory) sessionFactory;

    TransactionOptions transactionOptions = TransactionOptions.builder()
            .readConcern(ReadConcern.MAJORITY)
            .writeConcern(WriteConcern.ACKNOWLEDGED)
            .build();
    mongoSessionFactory.inTransaction(transactionOptions, session -> {
        System.out.println("Hello World");
    });
  }

  @Entity(name = "Book")
  static class Book {
    @Id
    int id;
    String title;
  }

}
