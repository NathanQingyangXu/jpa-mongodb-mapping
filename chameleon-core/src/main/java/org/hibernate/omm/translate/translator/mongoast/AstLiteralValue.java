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

import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.BsonValueCodec;
import org.bson.codecs.EncoderContext;

public record AstLiteralValue(BsonValue literalValue) implements AstValue {
    @Override
    public AstNodeType nodeType() {
        // TODO: what does Linq3 AST use for literal values?
        throw new UnsupportedOperationException();
    }

    @Override
    public void render(final BsonWriter writer) {
        new BsonValueCodec()
                .encode(writer, literalValue, EncoderContext.builder().build());
    }
}
