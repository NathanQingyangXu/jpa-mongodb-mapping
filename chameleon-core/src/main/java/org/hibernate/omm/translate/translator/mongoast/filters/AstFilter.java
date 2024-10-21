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

package org.hibernate.omm.translate.translator.mongoast.filters;

import org.hibernate.omm.translate.translator.mongoast.AstNode;

public interface AstFilter extends AstNode {
    /**
     * @return true if this is filtering by equality of the _id field.  Useful for determining whether it's safe to do an
     * updateOne/deleteOne instead of updateMany/deleteMany.  The former is preferred if possible because the server
     * can automatically retry updateOne/deleteOne but not updateMany/deleteMany.
     */
    default boolean isIdEqualityFilter() {
        return false;
    }
}
