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

package org.hibernate.omm.translate.translator.mongoast.stages;

import org.hibernate.omm.translate.translator.mongoast.AstNode;
import org.hibernate.omm.translate.translator.mongoast.expressions.AstExpression;

public interface AstProjectStageSpecification extends AstNode {
    static AstProjectStageSpecification Exclude(String path) {
        return new AstProjectStageExcludeFieldSpecification(path);
    }

    static AstProjectStageSpecification ExcludeId() {
        return new AstProjectStageExcludeFieldSpecification("_id");
    }

    static AstProjectStageSpecification Include(String path) {
        return new AstProjectStageIncludeFieldSpecification(path);
    }

    static AstProjectStageSpecification Set(String path, AstExpression value) {
        return new AstProjectStageSetFieldSpecification(path, value);
    }
}
