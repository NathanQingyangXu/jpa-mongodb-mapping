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

package org.hibernate.omm.join;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.omm.extension.MongoIntegrationTest;
import org.hibernate.omm.extension.SessionFactoryInjected;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 */
@MongoIntegrationTest
class SimpleJoinTests {

    @SessionFactoryInjected
    static SessionFactory sessionFactory;

    @Test
    void test() {
        var country = new Country();
        country.id = 1;
        country.name = "China";
        var province = new Province();
        province.id = 2;
        province.country = country;
        province.name = "Jilin";

        sessionFactory.inTransaction(session -> {
            City city = new City();
            city.id = 3;
            city.province = province;
            city.name = "Changchun";
            session.persist(country);
            session.persist(province);
            session.persist(city);
        });

        City city = new City();

        // the following Bson command will be issued:
        // { aggregate: "cities", pipeline: [ { $lookup: { from: "province", localField: "province_id",
        // foreignField: "_id", as: "p1_0", pipeline: [ { $lookup: { from: "countries", localField:
        // "country_id", foreignField: "_id", as: "c2_0" } }, { $unwind: "$c2_0" } ] } }, { $unwind:
        // "$p1_0" }, { $match: { _id: { $eq: ? } } }, { $project: { f0: "$_id", f1: "$name", f2:
        // "$p1_0._id", f3: "$p1_0.c2_0._id", f4: "$p1_0.c2_0.name", f5: "$p1_0.name", _id: 0 } } ] }
        sessionFactory.inTransaction(session -> session.load(city, 3));

        assertThat(city.province).usingRecursiveComparison().isEqualTo(province);
    }

    @Entity(name = "Country")
    static class Country {
        @Id
        int id;

        String name;
    }

    @Entity(name = "Province")
    static class Province {
        @Id
        int id;

        @ManyToOne
        @Fetch(FetchMode.JOIN)
        Country country;

        String name;
    }

    @Entity(name = "City")
    static class City {
        @Id
        int id;

        @ManyToOne
        @Fetch(FetchMode.JOIN)
        Province province;

        String name;
    }
}
