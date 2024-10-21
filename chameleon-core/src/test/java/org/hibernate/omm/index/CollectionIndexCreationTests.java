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

package org.hibernate.omm.index;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.BsonDocument;
import org.hibernate.omm.extension.CommandRecorderInjected;
import org.hibernate.omm.extension.HibernateProperty;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.service.CommandRecorder;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
@MongoIntegrationTest(
        externalEntities = Book.class,
        hibernateProperties = {
            @HibernateProperty(key = "jakarta.persistence.schema-generation.database.action", value = "create")
        })
class CollectionIndexCreationTests {

    @CommandRecorderInjected
    CommandRecorder commandRecorder;

    @Test
    void test_index_created() {
        final var commands = commandRecorder.getCommandsRecorded();
        assertThat(commands).allSatisfy(command -> assertThat(
                        command.getString("createIndexes").getValue())
                .isEqualTo("books"));
        assertThat(commands)
                .extracting(command -> command.getArray("indexes"))
                .allSatisfy(indexes -> assertThat(indexes).hasSize(1));
        assertThat(commands)
                .flatExtracting(command -> command.getArray("indexes"))
                .contains(
                        BsonDocument.parse("{ name: \"idx_on_single_col\", key: {publishYear: 1}, unique: false}"),
                        BsonDocument.parse(
                                "{ name: \"idx_on_multi_cols\", key: {publisher: 1, author: 1}, unique: false}"),
                        BsonDocument.parse("{ name: \"uniq_idx_on_single_col\", key: {isbn: 1}, unique: true}"),
                        BsonDocument.parse(
                                "{ name: \"uniq_idx_on_multi_cols\", key: {publisher: 1, title: 1}, unique: true}"));
    }
}
