/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.omm.exception;

import com.mongodb.assertions.Assertions;
import org.hibernate.omm.util.CollectionUtil;

import java.util.List;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoConfigMissingException extends RuntimeException {
    private final List<String> missingConfigs;

    public MongoConfigMissingException(final List<String> missingConfigs) {
        Assertions.assertTrue(CollectionUtil.isNotEmpty(missingConfigs));
        this.missingConfigs = missingConfigs;
    }

    @Override
    public String getMessage() {
        return String.format("Mandatory Mongo configuration%s missing: %s", (missingConfigs.size() > 1 ? "s" : ""), missingConfigs);
    }
}
