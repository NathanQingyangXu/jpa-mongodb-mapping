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

package org.hibernate.omm.type.aray;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.List;
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
class SimpleArrayFieldTests {

    @SessionFactoryInjected
    static SessionFactory sessionFactory;

    @MongoDatabaseInjected
    static MongoDatabase mongoDatabase;

    final Integer id = 22433;

    @Test
    @DisplayName("when collection field was persisted, it should be saved as embedded array in MongoDB")
    void test_persist_into_mongo_embedded_array_field() {
        var insertedMovie = sessionFactory.fromTransaction(session -> {
            var movie = new Movie(id);
            movie.tags = List.of("romantic", "classic");
            session.persist(movie);
            return movie;
        });

        var doc = mongoDatabase.getCollection("movies").find(Filters.eq(id)).first();

        assertThat(doc).isNotNull();
        assertThat(doc.getList("tags", String.class)).isEqualTo(insertedMovie.tags);
    }

    @Test
    @DisplayName("when collection field was persisted, it should be fetched as expected")
    void test_load_from_mongo_embedded_array_field() {
        var insertedMovie = sessionFactory.fromTransaction(session -> {
            var movie = new Movie(id);
            movie.tags = List.of("romantic", "classic");
            session.persist(movie);
            return movie;
        });
        sessionFactory.inTransaction(session -> {
            var loadedMovie = new Movie();
            session.load(loadedMovie, id);
            assertThat(loadedMovie).usingRecursiveComparison().isEqualTo(insertedMovie);
        });
    }

    @Test
    @DisplayName("when collection field was queried by some entry, it should return result as expected")
    void test_query_mongo_embedded_array_field() {
        var romanticTag = "romantic";
        var classicTag = "classic";

        sessionFactory.inTransaction(session -> {
            var movieWithRomanticTagOnly = new Movie(1);
            movieWithRomanticTagOnly.tags = List.of(romanticTag);
            session.persist(movieWithRomanticTagOnly);

            var movieWithClassicTagOnly = new Movie(2);
            movieWithClassicTagOnly.tags = List.of(classicTag);
            session.persist(movieWithClassicTagOnly);

            var movieWithBothTags = new Movie(3);
            movieWithBothTags.tags = List.of(romanticTag, classicTag);
            session.persist(movieWithBothTags);

            var movieWithNoTags = new Movie(4);
            movieWithNoTags.tags = Collections.emptyList();
            session.persist(movieWithNoTags);
        });

        sessionFactory.inTransaction(session -> {
            var queryStr = "from Movie m where array_contains(m.tags, :tag)";

            var classicMovies = session.createQuery(queryStr, Movie.class)
                    .setParameter("tag", classicTag)
                    .list();
            assertThat(classicMovies).extracting(Movie::getId).containsExactly(2, 3);

            var romanticMovies = session.createQuery(queryStr, Movie.class)
                    .setParameter("tag", romanticTag)
                    .list();
            assertThat(romanticMovies).extracting(Movie::getId).containsExactly(1, 3);
        });

        sessionFactory.inTransaction(session -> {
            var queryStr = "from Movie m where array_includes(m.tags, :tags)";
            var classicRomanticMovies = session.createQuery(queryStr, Movie.class)
                    .setParameter("tags", new String[] {classicTag, romanticTag})
                    .list();
            assertThat(classicRomanticMovies).extracting(Movie::getId).containsExactly(3);
        });
    }

    @Entity(name = "Movie")
    @Table(name = "movies")
    static class Movie {
        @Id
        Integer id;

        Movie() {}

        Movie(Integer id) {
            this.id = id;
        }

        Integer getId() {
            return id;
        }

        List<String> tags;
    }
}
