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

public record AstLookupStage(String from, String as, AstLookupStageMatch match, List<AstStage> pipeline)
        implements AstStage {
    @Override
    public AstNodeType nodeType() {
        return AstNodeType.LookupStage;
    }

    @Override
    public void render(final BsonWriter writer) {
        writer.writeStartDocument();
        writer.writeName("$lookup");
        writer.writeStartDocument();
        writer.writeString("from", from);
        writer.writeString("as", as);
        match.render(writer);
        if (!pipeline.isEmpty()) {
            writer.writeName("pipeline");
            writer.writeStartArray();
            pipeline.forEach(stage -> stage.render(writer));
            writer.writeEndArray();
        }
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    public AstLookupStage addPipeline(List<AstStage> pipeline) {
        if (!this.pipeline.isEmpty()) {
            throw new IllegalStateException("Already has a pipeline!");
        }
        return new AstLookupStage(from, as, match, pipeline);
    }
}
