/*
 * Copyright 2008-present MongoDB, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hibernate.omm.query;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.Test;

/**
 * @author Jeff Yemin
 * @author Nathan Xu
 * @since 1.0.0
 */
@MongoIntegrationTest
class OrderByTests {

    @SessionFactoryInjected
    SessionFactory sessionFactory;

    @Test
    void test() {
        var book1 = new Book();
        book1.id = 1L;
        book1.title = "War and Peace";
        book1.author = "Leo Tolstoy";
        book1.publishYear = 1869;

        var book2 = new Book();
        book2.id = 2L;
        book2.title = "The Idiot";
        book2.author = "Fyodor Dostoevsky";
        book2.publishYear = 1869;

        sessionFactory.inTransaction(session -> {
            session.persist(book1);
            session.persist(book2);
        });

        sessionFactory.inSession(session -> {
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

    @Entity(name = "Book")
    static class Book {

        @Id
        Long id;

        String title;

        String author;

        int publishYear;
    }
}
