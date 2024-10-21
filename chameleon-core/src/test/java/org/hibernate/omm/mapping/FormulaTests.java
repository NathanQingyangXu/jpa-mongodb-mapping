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

package org.hibernate.omm.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Formula;
import org.hibernate.omm.extension.MongoDatabaseInjected;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
@MongoIntegrationTest
class FormulaTests {

    @SessionFactoryInjected
    SessionFactory sessionFactory;

    @MongoDatabaseInjected
    MongoDatabase mongoDatabase;

    @Test
    @DisplayName("entity field annotated with @Formula will be computed based on provided Mongo aggregate expression")
    void test() {
        var id = 1234L;
        var credit = 200.0;
        var rate = 0.01;
        sessionFactory.inTransaction(session -> {
            var account = new Account(id);
            account.credit = credit;
            account.rate = rate;
            session.persist(account);
        });

        var doc = mongoDatabase.getCollection("accounts").find(Filters.eq(id)).first();

        assertThat(doc).isNotNull();
        assertThat(doc.keySet()).doesNotContain("interest");

        sessionFactory.inTransaction(session -> {
            var loadedAccount = new Account();
            session.load(loadedAccount, id);
            assertThat(loadedAccount.interest).isEqualTo(credit * rate);
        });
    }

    @Entity(name = "Account")
    @Table(name = "accounts")
    static class Account {

        @Id
        Long id;

        Double credit;

        Double rate;

        @Formula("{ $multiply: [ \"$credit\", \"$rate\" ] }")
        Double interest;

        Account() {}

        Account(Long id) {
            this.id = id;
        }
    }
}
