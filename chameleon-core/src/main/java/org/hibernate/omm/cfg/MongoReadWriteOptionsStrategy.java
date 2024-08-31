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

package org.hibernate.omm.cfg;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;
import org.bson.BsonDocument;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public interface MongoReadWriteOptionsStrategy {

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // common configurations for both transaction and non-transaction cases
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    default @Nullable ReadPreference clientReadPreference() {
        return null;
    }

    default @Nullable ReadConcern clientReadConcern() {
        return null;
    }

    default @Nullable WriteConcern clientWriteConcern() {
        return null;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // configurations in transaction
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    default @Nullable TransactionOptions sessionDefaultTransactionOptions(MongoClient client) {
        return null;
    }

    default @Nullable TransactionOptions transactionOptions(ClientSession session) {
        return null;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // configurations outside transaction
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    default @Nullable ReadPreference databaseReadPreference() {
        return null;
    }

    default @Nullable ReadConcern databaseReadConcern() {
        return null;
    }

    default @Nullable WriteConcern databaseWriteConcern() {
        return null;
    }

    default @Nullable ReadPreference collectionReadPreference(MongoCollection<BsonDocument> collection) {
        return null;
    }

    default @Nullable ReadConcern collectionReadConcern(MongoCollection<BsonDocument> collection) {
        return null;
    }

    default @Nullable WriteConcern collectionWriteConcern(MongoCollection<BsonDocument> collection) {
        return null;
    }

    default @Nullable ReadConcern readConcern(BsonDocument command) {
        return null;
    }

    default @Nullable WriteConcern writeConcern(BsonDocument command) {
        return null;
    }

}
