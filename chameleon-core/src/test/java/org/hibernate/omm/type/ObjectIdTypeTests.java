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

package org.hibernate.omm.type;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.bson.types.ObjectId;
import org.hibernate.SessionFactory;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
@MongoIntegrationTest
class ObjectIdTypeTests {

    final ObjectId objectId = ObjectId.get();

    @SessionFactoryInjected
    SessionFactory sessionFactory;

    Book insertBook() {
        return sessionFactory.fromTransaction(session -> {
            var book = new Book();
            book.id = objectId;
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
        // { insert: "books", documents: [ { author: ?, publishYear: ?, title: ?, _id: ? } ] }
        var insertedBook = insertBook();

        sessionFactory.inTransaction(session -> {
            var query = session.createQuery("from Book where id = :id", Book.class);
            query.setParameter("id", objectId);
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
            var book = session.getReference(Book.class, objectId);
            session.remove(book);
        });
        sessionFactory.inTransaction(session -> {
            var query = session.createSelectionQuery("from Book where id = :id", Book.class);
            query.setParameter("id", objectId);
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
            session.load(book, objectId);
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
            query.setParameter("id", objectId);
            var book = query.getSingleResult();
            assertThat(book).usingRecursiveComparison().isEqualTo(insertedBook);
        });
    }

    @Test
    void testUpdate() {
        insertBook();
        var newAuthor = "Fyodor Dostoevsky";
        var newTitle = "Crime and Punishment";
        int newPublishYear = 1866;

        // the following JSON command will be issued:
        // { update: "books", updates: [ { q: { _id: { $eq: ? } }, u: { $set: { author: ?, publishYear:
        // ?, title: ? } } } ] }
        sessionFactory.inTransaction(session -> {
            var book = new Book();
            session.load(book, objectId);
            book.author = newAuthor;
            book.title = newTitle;
            book.publishYear = newPublishYear;
            session.persist(book);
        });
        sessionFactory.inSession(session -> {
            var query = session.createQuery("from Book where id = :id", Book.class);
            query.setParameter("id", objectId);
            var book = query.getSingleResult();
            assertThat(book)
                    .extracting("author", "title", "publishYear")
                    .containsOnly(newAuthor, newTitle, newPublishYear);
        });
    }

    @Entity(name = "Book")
    static class Book {

        @Id
        ObjectId id;

        String title;

        String author;

        int publishYear;
    }
}
