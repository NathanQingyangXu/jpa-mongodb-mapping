package org.hibernate.omm.translate.translator;

import static com.mongodb.assertions.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.omm.translate.translator.mongoast.AstFieldUpdate;
import org.hibernate.omm.translate.translator.mongoast.AstSortField;
import org.hibernate.omm.translate.translator.mongoast.AstValue;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFilter;
import org.hibernate.omm.translate.translator.mongoast.stages.AstLookupStage;
import org.hibernate.omm.translate.translator.mongoast.stages.AstProjectStageSpecification;
import org.hibernate.omm.translate.translator.mongoast.stages.AstStage;

public class AttachmentKeys {

    private static final AttachmentKey<String> COLLECTION_NAME = new DefaultAttachmentKey<>("collectionName");
    private static final AttachmentKey<List<AstProjectStageSpecification>> PROJECT_STAGE_SPECIFICATIONS =
            new DefaultAttachmentKey<>("projectStageSpecifications");
    private static final AttachmentKey<String> FIELD_NAME = new DefaultAttachmentKey<>("fieldName");
    private static final AttachmentKey<AstValue> FIELD_VALUE = new DefaultAttachmentKey<>("fieldValue");
    private static final AttachmentKey<AstFilter> FILTER = new DefaultAttachmentKey<>("filter");
    private static final AttachmentKey<List<AstSortField>> SORT_FIELDS = new DefaultAttachmentKey<>("sortFields");
    private static final AttachmentKey<AstSortField> SORT_FIELD = new DefaultAttachmentKey<>("sortField");
    private static final AttachmentKey<List<AstStage>> JOIN_STAGES = new DefaultAttachmentKey<>("joinStages");
    private static final AttachmentKey<AstLookupStage> LOOKUP_STAGE = new DefaultAttachmentKey<>("lookupStage");
    private static final AttachmentKey<CollectionNameAndJoinStages> COLLECTION_NAME_AND_JOIN_STAGES =
            new DefaultAttachmentKey<>("collectionNameAndJoinStages");
    private static final AttachmentKey<List<AstFieldUpdate>> FIELD_UPDATES = new DefaultAttachmentKey<>("fieldUpdates");
    private static final AttachmentKey<AstFieldUpdate> FIELD_UPDATE = new DefaultAttachmentKey<>("fieldUpdate");

    public static AttachmentKey<String> collectionName() {
        return COLLECTION_NAME;
    }

    public static AttachmentKey<List<AstProjectStageSpecification>> projectStageSpecifications() {
        return PROJECT_STAGE_SPECIFICATIONS;
    }

    public static AttachmentKey<String> fieldName() {
        return FIELD_NAME;
    }

    public static AttachmentKey<AstValue> fieldValue() {
        return FIELD_VALUE;
    }

    public static AttachmentKey<AstFilter> filter() {
        return FILTER;
    }

    public static AttachmentKey<List<AstSortField>> sortFields() {
        return SORT_FIELDS;
    }

    public static AttachmentKey<AstSortField> sortField() {
        return SORT_FIELD;
    }

    public static AttachmentKey<List<AstStage>> joinStages() {
        return JOIN_STAGES;
    }

    public static AttachmentKey<AstLookupStage> lookupStage() {
        return LOOKUP_STAGE;
    }

    public static AttachmentKey<CollectionNameAndJoinStages> collectionNameAndJoinStages() {
        return COLLECTION_NAME_AND_JOIN_STAGES;
    }

    public static AttachmentKey<List<AstFieldUpdate>> fieldUpdates() {
        return FIELD_UPDATES;
    }

    public static AttachmentKey<AstFieldUpdate> fieldUpdate() {
        return FIELD_UPDATE;
    }

    private static final class DefaultAttachmentKey<V> implements AttachmentKey<V> {
        private static final Set<String> AVOID_KEY_DUPLICATION = new HashSet<>();

        private final String key;

        private DefaultAttachmentKey(final String key) {
            assertTrue(AVOID_KEY_DUPLICATION.add(key));
            this.key = key;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DefaultAttachmentKey<?> that = (DefaultAttachmentKey<?>) o;
            return key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return key;
        }
    }
}
