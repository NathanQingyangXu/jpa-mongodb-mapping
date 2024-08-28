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

package org.hibernate.omm.array;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.BsonDocument;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Struct;
import org.hibernate.omm.extension.CommandRecorderInjected;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.hibernate.omm.service.CommandRecorder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nathan Xu
 * @author Jeff Yemin
 * @since 1.0.0
 */
@MongoIntegrationTest
class StructTests {

    @SessionFactoryInjected
    SessionFactory sessionFactory;

    @CommandRecorderInjected
    CommandRecorder commandRecorder;

    @Test
    void test_persist() {
        sessionFactory.inTransaction(session -> {
            var tag = new TagsByAuthor();
            tag.author = "Nathan";
            tag.tags = List.of("comedy", "drama");
            var movie = new Movie();
            movie.id = 1;
            movie.title = "Forrest Gump";
            movie.tagsByAuthor = List.of(tag);
            session.persist(movie);
        });
        assertThat(commandRecorder.getCommandsRecorded()).singleElement().satisfies(
                command -> {
                    assertThat(command.getFirstKey()).isEqualTo("insert");
                    assertThat(command.getString(command.getFirstKey()).getValue()).isEqualTo("movies");
                    assertThat(command.getArray("documents").getValues()).singleElement().satisfies(
                            document -> {
                                final var expectedJson = """
                                        {"tagsByAuthor": [{"commenter": "Nathan", "tags": ["comedy", "drama"]}], "title": "Forrest Gump", "_id": 1}
                                        """;
                                assertThat(document).isEqualTo(BsonDocument.parse(expectedJson));
                            }
                    );
                }
        );
    }

    @Entity(name = "Movie")
    @Table(name = "movies")
    static class Movie {
        @Id
        int id;

        String title;

        List<TagsByAuthor> tagsByAuthor;
    }

    @Embeddable
    @Struct(name = "TagsByAuthor")
    static class TagsByAuthor {

        @Column(name = "commenter")
        String author;

        List<String> tags;

        @Override
        public String toString() {
            return "TagsByAuthor{" +
                    "author='" + author + '\'' +
                    ", tags=" + tags +
                    '}';
        }
    }
}
