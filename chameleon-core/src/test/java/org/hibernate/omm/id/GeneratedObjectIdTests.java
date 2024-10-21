/*
 *
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

package org.hibernate.omm.id;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.bson.BsonValue;
import org.bson.types.ObjectId;
import org.hibernate.SessionFactory;
import org.hibernate.omm.annotations.ObjectIdGenerator;
import org.hibernate.omm.extension.CommandRecorderInjected;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.hibernate.omm.service.CommandRecorder;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
@MongoIntegrationTest
class GeneratedObjectIdTests {

    @SessionFactoryInjected
    SessionFactory sessionFactory;

    @CommandRecorderInjected
    CommandRecorder commandRecorder;

    @Test
    void test() {
        sessionFactory.inTransaction(session -> {
            var book = new Book();
            book.title = "Moby-Dick";
            session.persist(book);
        });
        assertThat(commandRecorder.getCommandsRecorded()).singleElement().satisfies(command -> {
            assertThat(command.getArray("documents")).singleElement().satisfies(doc -> {
                assertThat(doc.isDocument()).isTrue();
                assertThat(doc.asDocument())
                        .overridingErrorMessage("'_id' field with ObjectId type should have been generated")
                        .containsKey("_id")
                        .extractingByKey("_id")
                        .isNotNull()
                        .extracting(BsonValue::asObjectId)
                        .isNotNull();
            });
        });
    }

    @Entity(name = "Book")
    static class Book {

        @Id
        @GeneratedValue
        @ObjectIdGenerator
        ObjectId id;

        String title;
    }
}
