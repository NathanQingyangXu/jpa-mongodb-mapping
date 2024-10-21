package org.hibernate.omm.crud;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SessionFactory;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 */
@MongoIntegrationTest
class SimpleCRUDTests {

    @SessionFactoryInjected
    static SessionFactory sessionFactory;

    final Long id = 1234L;

    Book insertBook() {
        return sessionFactory.fromTransaction(session -> {
            var book = new Book();
            book.id = id;
            book.title = "War and Peace";
            book.author = "Leo Tolstoy";
            book.publishYear = 1869;
            session.persist(book);
            return book;
        });
    }

    @Test
    void testInsert() {

        // the following JSON command will be issued:
        // { aggregate: "books", pipeline: [ { $match: { _id: { $eq: ? } }}, { $project: { _id: 1,
        // author: 1, publishYear: 1, title: 1 } } ] }
        var insertedBook = insertBook();

        sessionFactory.inTransaction(session -> {
            var query = session.createQuery("from Book where id = :id", Book.class);
            query.setParameter("id", id);
            var book = query.getSingleResult();
            assertThat(book).usingRecursiveComparison().isEqualTo(insertedBook);
        });
    }

    @Test
    void testDelete() {

        insertBook();

        // the following JSON command will be issued:
        // { delete: "books", deletes: [ { q: { _id: { $eq: ? } }, limit: 0 } ] }
        sessionFactory.inTransaction(session -> {
            var book = session.getReference(Book.class, id);
            session.remove(book);
        });
        sessionFactory.inTransaction(session -> {
            var query = session.createSelectionQuery("from Book where id = :id", Book.class);
            query.setParameter("id", id);
            assertThat(query.getResultList()).isEmpty();
        });
    }

    @Test
    void testLoad() {

        var insertedBook = insertBook();

        // the following JSON command will be issued:
        // { aggregate: "books", pipeline: [ { $match: { _id: { $eq: ? } }}, { $project: { _id: 1,
        // author: 1, publishYear: 1, title: 1 } } ] }
        sessionFactory.inTransaction(session -> {
            var book = new Book();
            session.load(book, id);
            assertThat(book).usingRecursiveComparison().isEqualTo(insertedBook);
        });
    }

    @Test
    void testQuery() {

        var insertedBook = insertBook();

        // the following JSON command will be issued:
        // { aggregate: "books", pipeline: [ { $match: { _id: { $eq: ? } }}, { $project: { _id: 1,
        // author: 1, publishYear: 1, title: 1 } } ] }
        sessionFactory.inTransaction(session -> {
            var query = session.createQuery("from Book where id = :id", Book.class);
            query.setParameter("id", id);
            var book = query.getSingleResult();
            assertThat(book).usingRecursiveComparison().isEqualTo(insertedBook);
        });
    }

    @Test
    void testPersist() {
        insertBook();
        var newAuthor = "Fyodor Dostoevsky";
        var newTitle = "Crime and Punishment";
        int newPublishYear = 1866;

        // the following JSON command will be issued:
        // { update: "books", updates: [ { q: { _id: { $eq: ? } }, u: { $set: { author: ?, publishYear:
        // ?, title: ? } } } ] }
        sessionFactory.inTransaction(session -> {
            var book = new Book();
            session.load(book, id);
            book.author = newAuthor;
            book.title = newTitle;
            book.publishYear = newPublishYear;
            session.persist(book);
        });
        sessionFactory.inSession(session -> {
            var query = session.createQuery("from Book where id = :id", Book.class);
            query.setParameter("id", id);
            var book = query.getSingleResult();
            assertThat(book)
                    .extracting("author", "title", "publishYear")
                    .containsOnly(newAuthor, newTitle, newPublishYear);
        });
    }

    @Test
    void testUpdateQuery() {
        insertBook();
        var newAuthor = "Fyodor Dostoevsky";
        var newTitle = "Crime and Punishment";
        int newPublishYear = 1866;

        // the following JSON command will be issued:
        // { update: "books", updates: [ { q: { _id: { $eq: ? } }, u: { $set: { author: ?, publishYear:
        // ?, title: ? } } } ] }
        sessionFactory.inTransaction(session -> {
            var update = session.createMutationQuery("update Book "
                    + "set author = :author, title = :title, publishYear = :publishYear " + "where id = :id");
            update.setParameter("id", id);
            update.setParameter("author", newAuthor);
            update.setParameter("title", newTitle);
            update.setParameter("publishYear", newPublishYear);
            update.executeUpdate();
        });
        sessionFactory.inSession(session -> {
            var query = session.createQuery("from Book where id = :id", Book.class);
            query.setParameter("id", id);
            var book = query.getSingleResult();
            assertThat(book)
                    .extracting("author", "title", "publishYear")
                    .containsOnly(newAuthor, newTitle, newPublishYear);
        });
    }

    @Entity(name = "Book")
    static class Book {

        @Id
        Long id;

        String title;

        String author;

        int publishYear;
    }
}
