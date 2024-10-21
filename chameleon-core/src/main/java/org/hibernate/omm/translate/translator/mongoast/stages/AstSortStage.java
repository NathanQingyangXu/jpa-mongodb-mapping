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

import java.util.List;
import org.bson.BsonWriter;
import org.hibernate.omm.translate.translator.mongoast.AstNodeType;
import org.hibernate.omm.translate.translator.mongoast.AstSortField;

public record AstSortStage(List<AstSortField> sortFields) implements AstStage {
    @Override
    public AstNodeType nodeType() {
        return AstNodeType.SortStage;
    }

    @Override
    public void render(final BsonWriter writer) {
        writer.writeStartDocument();
        writer.writeName("$sort");
        writer.writeStartDocument();
        sortFields.forEach(sortField -> sortField.render(writer));
        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
