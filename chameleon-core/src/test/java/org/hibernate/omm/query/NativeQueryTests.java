/*
 * Copyright 2024-present MongoDB, Inc.
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
import jakarta.persistence.Table;
import org.hibernate.SessionFactory;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 */
@MongoIntegrationTest
class NativeQueryTests {

    @SessionFactoryInjected
    static SessionFactory sessionFactory;

    @Test
    @Disabled("the testing will succeed only if we enable 'hibernate-overlay' project dependency")
    void test_with_parameter() {
        var insertedBook = sessionFactory.fromTransaction(session -> {
            var book = new Book();
            book.id = 1234L;
            book.title = "War and Peace";
            book.author = "Leo Tolstoy";
            book.publishYear = 1869;
            session.persist(book);
            return book;
        });

        var nativeQuery =
                """
                {
                    aggregate: "books",
                    pipeline: [
                        { $match :  { _id: { $eq: ? } } },
                        { $project: { _id: 1, publishYear: 1, title: 1, author: 1 } }
                    ]
                }
                """;
        sessionFactory.inTransaction(session -> {
            var query = session.createNativeQuery(nativeQuery, Book.class).setParameter(1, insertedBook.id);
            var book = query.getSingleResult();
            assertThat(book).usingRecursiveComparison().isEqualTo(insertedBook);
        });
    }

    @Test
    void test_without_parameter() {
        var insertedBook = sessionFactory.fromTransaction(session -> {
            var book = new Book();
            book.id = 1234L;
            book.title = "War and Peace";
            book.author = "Leo Tolstoy";
            book.publishYear = 1869;
            session.persist(book);
            return book;
        });

        var nativeQuery = "{ aggregate: \"books\", pipeline: [" + "{ $match :  { _id: { $eq: 1234 } } }, "
                + "{ $project: { _id: 1, publishYear: 1, title: 1, author: 1 } }"
                + "] }";
        sessionFactory.inTransaction(session -> {
            var query = session.createNativeQuery(nativeQuery, Book.class);
            var book = query.getSingleResult();
            assertThat(book).usingRecursiveComparison().isEqualTo(insertedBook);
        });
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
