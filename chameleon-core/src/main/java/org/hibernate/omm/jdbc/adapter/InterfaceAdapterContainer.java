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

package org.hibernate.omm.jdbc.adapter;

import org.hibernate.omm.annotation.JdbcInterfaceAdapter;
import org.hibernate.omm.annotation.JdbcInterfaceAdapters;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
@JdbcInterfaceAdapters({
    @JdbcInterfaceAdapter(
            interfaceName = "java.sql.Statement",
            adapterClassName = "org.hibernate.omm.jdbc.adapter" + ".StatementAdapter"),
    @JdbcInterfaceAdapter(
            interfaceName = "java.sql.ResultSet",
            adapterClassName = "org.hibernate.omm.jdbc.adapter" + ".ResultSetAdapter"),
    @JdbcInterfaceAdapter(
            interfaceName = "java.sql.PreparedStatement",
            adapterClassName = "org.hibernate.omm.jdbc.adapter" + ".PreparedStatementAdapter",
            overrideDeclaredMethodsOnly = true),
    @JdbcInterfaceAdapter(
            interfaceName = "java.sql.Array",
            adapterClassName = "org.hibernate.omm.jdbc.adapter.ArrayAdapter"),
    @JdbcInterfaceAdapter(
            interfaceName = "java.sql.Connection",
            adapterClassName = "org.hibernate.omm.jdbc.adapter" + ".ConnectionAdapter"),
    @JdbcInterfaceAdapter(
            interfaceName = "java.sql.DatabaseMetaData",
            adapterClassName = "org.hibernate.omm.jdbc.adapter" + ".DatabaseMetaDataAdapter"),
    @JdbcInterfaceAdapter(
            interfaceName = "java.sql.ResultSetMetaData",
            adapterClassName = "org.hibernate.omm.jdbc.adapter" + ".ResultSetMetaDataAdapter")
})
public interface InterfaceAdapterContainer {}
