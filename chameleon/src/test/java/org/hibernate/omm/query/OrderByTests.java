package org.hibernate.omm.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.omm.AbstractMongodbIntegrationTests;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nathan Xu
 */
class OrderByTests extends AbstractMongodbIntegrationTests {

    @Test
    void testOrderBy() {
        var book1 = new Book();
        book1.id = 1L;
        book1.title = "War and Peace";
        book1.author = "Leo Tolstoy";
        book1.publishYear = 1869;

        var book2 = new Book();
        book2.id = 2L;
        book2.title = "Crime and Punishment";
        book2.author = "Leo Tolstoy";
        book2.publishYear = 1869;

        getSessionFactory().inTransaction(session -> {
            session.persist(book1);
            session.persist(book2);
        });

        getSessionFactory().inSession(session -> {
            var query = session.createQuery("from Book b order by b.title asc", Book.class);
            var books = query.getResultList();
            assertThat(books).usingRecursiveComparison().isEqualTo(List.of(book2, book1));

            query = session.createQuery("from Book b order by b.title desc", Book.class);
            books = query.getResultList();
            assertThat(books).usingRecursiveComparison().isEqualTo(List.of(book1, book2));

            query = session.createQuery("from Book b order by b.publishYear, b.title desc", Book.class);
            books = query.getResultList();
            assertThat(books).usingRecursiveComparison().isEqualTo(List.of(book1, book2));
        });
    }

    @Override
    public List<Class<?>> getAnnotatedClasses() {
        return List.of(Book.class);
    }

    @Entity(name = "Book")
    @Table(name = "books")
    static class Book {

        @Id
        Long id;

        String title;

        String author;

        int publishYear;

    }
}
