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

package org.hibernate.omm.id;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.SessionFactory;
import org.hibernate.omm.extension.MongoDatabaseInjected;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 */
@MongoIntegrationTest
class MongoIdColumnNameTests {

    @SessionFactoryInjected
    static SessionFactory sessionFactory;

    @MongoDatabaseInjected
    static MongoDatabase mongoDatabase;

    final Long id = 21344L;

    @Test
    @DisplayName("when @Id field was not annotated with @Column(name = \"xxx\")")
    void test_implicit_id_column_spec() {

        sessionFactory.inTransaction(session -> {
            var entity = new WithImplicitIdColumnSpec();
            entity.id = id;
            entity.title = "Bible";
            session.persist(entity);
        });

        assertThat(mongoDatabase.getCollection("books").find(Filters.eq(id)).first())
                .isNotNull();
    }

    @Test
    @DisplayName("when @Id field was annotated with @Column(name = \"xxx\")")
    void test_explicit_id_column_spec() {
        sessionFactory.inTransaction(session -> {
            var entity = new WithExplicitIdColumnSpec();
            entity.id = id;
            entity.title = "Bible";
            session.persist(entity);
        });

        assertThat(mongoDatabase.getCollection("books").find(Filters.eq(id)).first())
                .isNotNull();
    }

    @Entity(name = "WithImplicitIdColumnSpec")
    @Table(name = "books")
    static class WithImplicitIdColumnSpec {

        @Id
        Long id;

        String title;
    }

    @Entity(name = "WithExplicitIdColumnSpec")
    @Table(name = "books")
    static class WithExplicitIdColumnSpec {

        @Id
        @Column(name = "identifier")
        Long id;

        String title;
    }
}
