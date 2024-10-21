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

package org.hibernate.omm.type.struct;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.bson.BsonDocument;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Struct;
import org.hibernate.omm.extension.CommandRecorderInjected;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.hibernate.omm.service.CommandRecorder;
import org.junit.jupiter.api.Test;

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
        var tagsByAuthor = new TagsByAuthor();
        tagsByAuthor.author = "Nathan";
        tagsByAuthor.tags = List.of("comedy", "drama");
        var movie = new Movie();
        movie.id = 1;
        movie.title = "Forrest Gump";
        movie.tagsByAuthorList = new TagsByAuthor[] {tagsByAuthor};
        sessionFactory.inTransaction(session -> session.persist(movie));
        assertThat(commandRecorder.getCommandsRecorded()).singleElement().satisfies(command -> {
            assertThat(command.getFirstKey()).isEqualTo("insert");
            assertThat(command.getString(command.getFirstKey()).getValue()).isEqualTo("movies");
            assertThat(command.getArray("documents").getValues())
                    .singleElement()
                    .satisfies(document -> {
                        final var expectedJson =
                                """
                                        {"tagsByAuthorList": [{"commenter": "Nathan", "tags": ["comedy", "drama"]}], "title": "Forrest Gump", "_id": 1}
                                        """;
                        assertThat(document).isEqualTo(BsonDocument.parse(expectedJson));
                    });
        });
    }

    @Test
    void test_query() {
        var tagsByAuthor = new TagsByAuthor();
        tagsByAuthor.author = "Nathan";
        tagsByAuthor.tags = List.of("comedy", "drama");
        var movie = new Movie();
        movie.id = 1;
        movie.title = "Forrest Gump";
        movie.tagsByAuthorList = new TagsByAuthor[] {tagsByAuthor};
        sessionFactory.inTransaction(session -> session.persist(movie));
        sessionFactory.inTransaction(session -> {
            final var fetchedMovie = session.createQuery("from Movie where id = :id", Movie.class)
                    .setParameter("id", 1)
                    .getSingleResult();
            assertThat(fetchedMovie).usingRecursiveComparison().isEqualTo(movie);
        });
    }

    @Entity(name = "Movie")
    @Table(name = "movies")
    static class Movie {
        @Id
        int id;

        String title;

        TagsByAuthor[] tagsByAuthorList;
    }

    @Embeddable
    @Struct(name = "TagsByAuthor")
    static class TagsByAuthor {

        @Column(name = "commenter")
        String author;

        List<String> tags;

        @Override
        public String toString() {
            return "TagsByAuthor{" + "author='" + author + '\'' + ", tags=" + tags + '}';
        }
    }
}
