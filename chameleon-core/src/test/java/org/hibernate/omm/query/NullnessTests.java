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
class NullnessTests {

    @SessionFactoryInjected
    SessionFactory sessionFactory;

    @Test
    void test() {
        var foo = new Foo();
        foo.id = 1L;
        foo.possiblyNull = "s";

        var foo2 = new Foo();
        foo2.id = 2L;
        foo2.possiblyNull = null;

        sessionFactory.inTransaction(session -> {
            session.persist(foo);
            session.persist(foo2);
        });

        sessionFactory.inSession(session -> {
            var query = session.createQuery("from Foo where possiblyNull is null", Foo.class);
            var res = query.getSingleResult();
            assertThat(res).usingRecursiveComparison().isEqualTo(foo2);

            query = session.createQuery("from Foo where possiblyNull is not null", Foo.class);
            res = query.getSingleResult();
            assertThat(res).usingRecursiveComparison().isEqualTo(foo);
        });
    }

    @Entity(name = "Foo")
    static class Foo {
        @Id
        Long id;

        String possiblyNull;
    }
}
