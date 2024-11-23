package org.hibernate.omm.data;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@MongoIntegrationTest(externalEntities = Book.class)
public class BasicHibernateDataTests {

  @SessionFactoryInjected
  static SessionFactory sessionFactory;

  private StatelessSession session;
  private BookRepository repository;

  @BeforeEach
  void setUp() {
    session = sessionFactory.openStatelessSession();
    repository = new BookRepository_(session);
  }

  @AfterEach
  void tearDown() {
    if (session != null) {
      session.close();
    }
  }

  @AfterAll
  static void shutDown() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
  }

  @Test
  void test_insert() {
    var id = 1L;
    var book = new Book(id, "War and Peace");
    repository.insert(book);
    assertThat(repository.find(id)).usingRecursiveComparison().isEqualTo(book);
  }

  @Test
  void test_delete() {
    var id = 2L;
    var book = new Book(id, "Fathers and Sons");
    repository.insert(book);
    repository.delete(book);
    assertThat(repository.find(id)).isNull();
  }

  @Test
  void test_update() {
    var id = 3L;
    var book = new Book(id, "Crime and Punishment");
    repository.insert(book);
    book.title = "War and Peace";
    repository.update(book);
    assertThat(repository.find(id)).usingRecursiveComparison().isEqualTo(book);
  }

  @Test
  void test_find() {
    var id = 4L;
    var book = new Book(id, "Idiot");
    repository.insert(book);
    assertThat(repository.find(id)).usingRecursiveComparison().isEqualTo(book);
  }

  @Test
  void test_save_insert() {
    var id = 5L;
    var book = new Book(id, "Notes from Deadhouse");
    repository.save(book);
    assertThat(repository.find(id)).usingRecursiveComparison().isEqualTo(book);
  }

  @Test
  @Disabled("enabled after we finish 'upsert' MQL translation")
  void test_save_update() {
    var id = 6L;
    var book = new Book(id, "White Night");
    repository.insert(book);
    book.title = "Childhood";
    repository.save(book);
    assertThat(repository.find(id)).usingRecursiveComparison().isEqualTo(book);
  }

}
