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

package org.hibernate.omm.translate.translator.mongoast;

import java.util.List;
import org.bson.BsonWriter;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFilter;

public record UpdateCommand(String collection, AstFilter filter, List<AstFieldUpdate> updates) implements AstNode {
    @Override
    public AstNodeType nodeType() {
        return AstNodeType.UpdateCommand;
    }

    @Override
    public void render(final BsonWriter writer) {
        writer.writeStartDocument();
        writer.writeString("update", collection);
        writer.writeName("updates");
        writer.writeStartArray();
        writer.writeStartDocument();
        writer.writeName("q");
        filter.render(writer);
        writer.writeName("u");
        writer.writeStartDocument();
        writer.writeName("$set");
        writer.writeStartDocument();
        updates.forEach(update -> update.render(writer));
        writer.writeEndDocument();
        writer.writeEndDocument();
        writer.writeBoolean("multi", !filter.isIdEqualityFilter());
        writer.writeEndDocument();
        writer.writeEndArray();
        writer.writeEndDocument();
    }
}
