package org.hibernate.omm.dialect.exporter;

import static com.mongodb.internal.operation.IndexHelper.generateIndexName;

import com.mongodb.lang.Nullable;
import java.util.List;
import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonElement;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Selectable;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public final class MongoIndexCommandUtil {

    private MongoIndexCommandUtil() {}

    public static BsonDocument getKeys(final Index index) {
        final var keys = new BsonDocument();
        for (Selectable selectable : index.getSelectables()) {
            if (!selectable.isFormula()) {
                keys.put(selectable.getText(), new BsonInt32(1));
            }
        }
        return keys;
    }

    public static BsonDocument getKeys(final Constraint constraint) {
        final var keys = new BsonDocument();
        for (Column column : constraint.getColumns()) {
            keys.put(column.getName(), new BsonInt32(1));
        }
        return keys;
    }

    public static BsonDocument getIndexCreationCommand(
            final String collection, @Nullable final String indexName, final BsonDocument keys, final boolean unique) {

        final var index = new BsonDocument(
                List.of(new BsonElement("key", keys), new BsonElement("unique", BsonBoolean.valueOf(unique))));
        if (indexName != null) {
            index.put("name", new BsonString(indexName));
        }

        return new BsonDocument(List.of(
                new BsonElement("createIndexes", new BsonString(collection)),
                new BsonElement("indexes", new BsonArray(List.of(index)))));
    }

    public static BsonDocument getIndexDeletionCommand(
            final String collection, @Nullable final String indexName, final BsonDocument keys) {
        return new BsonDocument(List.of(
                new BsonElement("dropIndexes", new BsonString(collection)),
                new BsonElement("index", new BsonString(indexName != null ? indexName : generateIndexName(keys)))));
    }
}
