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

package org.hibernate.omm.jdbc.adapter;

import org.hibernate.omm.annotation.InterfaceAdapter;
import org.hibernate.omm.annotation.InterfaceAdapters;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
@InterfaceAdapters(
        {
                @InterfaceAdapter(interfaceName = "java.sql.Statement", adapterClassName = "org.hibernate.omm.jdbc.adapter" +
                        ".StatementAdapter"),
                @InterfaceAdapter(interfaceName = "java.sql.ResultSet", adapterClassName = "org.hibernate.omm.jdbc.adapter" +
                        ".ResultSetAdapter"),
                @InterfaceAdapter(interfaceName = "java.sql.PreparedStatement", adapterClassName = "org.hibernate.omm.jdbc.adapter" +
                        ".PreparedStatementAdapter", overrideDeclaredMethodsOnly = true),
                @InterfaceAdapter(interfaceName = "java.sql.Array", adapterClassName = "org.hibernate.omm.jdbc.adapter.ArrayAdapter"),
                @InterfaceAdapter(interfaceName = "java.sql.CallableStatement", adapterClassName = "org.hibernate.omm.jdbc.adapter" +
                        ".CallableStatementAdapter"),
                @InterfaceAdapter(interfaceName = "java.sql.Connection", adapterClassName = "org.hibernate.omm.jdbc.adapter" +
                        ".ConnectionAdapter"),
                @InterfaceAdapter(interfaceName = "java.sql.DatabaseMetaData", adapterClassName = "org.hibernate.omm.jdbc.adapter" +
                        ".DatabaseMetaDataAdapter"),
                @InterfaceAdapter(interfaceName = "java.sql.ResultSetMetaData", adapterClassName = "org.hibernate.omm.jdbc.adapter" +
                        ".ResultSetMetaDataAdapter"),
                @InterfaceAdapter(interfaceName = "java.sql.Driver", adapterClassName = "org.hibernate.omm.jdbc.adapter.DriverAdapter")

        }
)
public interface InterfaceAdapterContainer {
}
