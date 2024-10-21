package org.hibernate.omm.crud;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SessionFactory;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
@MongoIntegrationTest
class DeleteTests {

    @SessionFactoryInjected
    SessionFactory sessionFactory;

    @Test
    void test_delete_all() {
        sessionFactory.inTransaction(session -> {
            var book1 = new Book(1L);
            var book2 = new Book(2L);
            var book3 = new Book(3L);
            session.persist(book1);
            session.persist(book2);
            session.persist(book3);
        });
        sessionFactory.inTransaction(session -> {
            var query = session.createMutationQuery("delete from Book");
            Assertions.assertDoesNotThrow(query::executeUpdate);
        });
        sessionFactory.inTransaction(session -> {
            var query = session.createSelectionQuery("from Book", Book.class);
            assertThat(query.getResultList()).isEmpty();
        });
    }

    @Test
    void test_delete_with_filter() {
        sessionFactory.inTransaction(session -> {
            var book1 = new Book(1L);
            var book2 = new Book(2L);
            var book3 = new Book(3L);
            session.persist(book1);
            session.persist(book2);
            session.persist(book3);
        });
        sessionFactory.inTransaction(session -> {
            var query = session.createMutationQuery("delete from Book where id = :id");
            query.setParameter("id", 1L);
            Assertions.assertDoesNotThrow(query::executeUpdate);
        });
        sessionFactory.inTransaction(session -> {
            var query = session.createSelectionQuery("from Book where id = :id", Book.class);
            query.setParameter("id", 1L);
            assertThat(query.getResultList()).isEmpty();
        });
    }

    @Entity(name = "Book")
    static class Book {
        @Id
        Long id;

        Book() {}

        Book(final Long id) {
            this.id = id;
        }
    }
}
