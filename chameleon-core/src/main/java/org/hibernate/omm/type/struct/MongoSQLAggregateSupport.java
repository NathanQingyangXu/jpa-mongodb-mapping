package org.hibernate.omm.type.struct;

import static org.hibernate.type.SqlTypes.STRUCT;
import static org.hibernate.type.SqlTypes.STRUCT_ARRAY;

import org.hibernate.dialect.aggregate.AggregateSupportImpl;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoSQLAggregateSupport extends AggregateSupportImpl {

    public static final MongoSQLAggregateSupport INSTANCE = new MongoSQLAggregateSupport();

    private MongoSQLAggregateSupport() {}

    @Override
    public String aggregateComponentCustomReadExpression(
            String template,
            String placeholder,
            String aggregateParentReadExpression,
            String columnExpression,
            AggregateColumn aggregateColumn,
            Column column) {
        switch (aggregateColumn.getTypeCode()) {
            case STRUCT:
            case STRUCT_ARRAY:
                return template.replace(placeholder, '(' + aggregateParentReadExpression + ")." + columnExpression);
        }
        throw new IllegalArgumentException("Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode());
    }

    @Override
    public String aggregateComponentAssignmentExpression(
            String aggregateParentAssignmentExpression,
            String columnExpression,
            AggregateColumn aggregateColumn,
            Column column) {
        switch (aggregateColumn.getTypeCode()) {
            case STRUCT:
            case STRUCT_ARRAY:
                return aggregateParentAssignmentExpression + "." + columnExpression;
        }
        throw new IllegalArgumentException("Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode());
    }

    @Override
    public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
        return false;
    }
}
