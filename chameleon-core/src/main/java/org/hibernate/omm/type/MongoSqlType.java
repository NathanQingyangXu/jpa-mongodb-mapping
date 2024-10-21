package org.hibernate.omm.type;

import org.hibernate.type.descriptor.java.JavaType;

/**
 * Simulate {@link java.sql.Types} so we can differentiate among different MongoDB types during invoking
 * {@link java.sql.PreparedStatement#setObject(int, Object, int)}
 *
 * @author Nathan Xu
 * @see ObjectIdJdbcType#getBinder(JavaType)
 * @since 1.0.0
 */
public final class MongoSqlType {
    private MongoSqlType() {}

    public static final int UNKNOWN = 4_000;
    public static final int OBJECT_ID = 3_000;
}
