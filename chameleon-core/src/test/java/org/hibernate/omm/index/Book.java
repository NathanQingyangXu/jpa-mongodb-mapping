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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
@Entity(name = "Book")
@Table(
        name = "books",
        indexes = {
            @Index(name = "idx_on_single_col", columnList = "publishYear"),
            @Index(name = "idx_on_multi_cols", columnList = "publisher,author"),
            @Index(name = "uniq_idx_on_single_col", columnList = "isbn", unique = true),
            @Index(name = "uniq_idx_on_multi_cols", columnList = "publisher,title", unique = true)
        })
class Book {

    @Id
    @GeneratedValue
    UUID id;

    String isbn;
    String author;
    String title;
    String publisher;
    int publishYear;
}
