package org.hibernate.omm.type;

import com.mongodb.lang.Nullable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bson.BsonObjectId;
import org.bson.assertions.Assertions;
import org.bson.types.ObjectId;
import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterCharacterData;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class ObjectIdJdbcType implements JdbcType {

    private static final ObjectIdJdbcType INSTANCE = new ObjectIdJdbcType();

    public static ObjectIdJdbcType getInstance() {
        return INSTANCE;
    }

    @Override
    public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
            @Nullable final Integer length, @Nullable final Integer scale, final TypeConfiguration typeConfiguration) {
        Assertions.notNull("typeConfiguration", typeConfiguration);
        return typeConfiguration.getJavaTypeRegistry().getDescriptor(ObjectId.class);
    }

    @Override
    public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(final JavaType<T> javaType) {
        Assertions.notNull("javaType", javaType);
        return new JdbcLiteralFormatterCharacterData<>(javaType, false);
    }

    @Override
    public Class<?> getPreferredJavaTypeClass(@Nullable final WrapperOptions options) {
        return ObjectId.class;
    }

    @Override
    public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
        Assertions.notNull("javaType", javaType);
        return new BasicBinder<>(javaType, this) {

            @Override
            protected void doBind(
                    final PreparedStatement st, final X value, final int index, final WrapperOptions options)
                    throws SQLException {
                st.setObject(index, value, MongoSqlType.OBJECT_ID);
            }

            @Override
            protected void doBind(
                    final CallableStatement st, final X value, final String name, final WrapperOptions options)
                    throws SQLException {
                throw new NotSupportedSQLException();
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
        Assertions.notNull("javaType", javaType);
        return new BasicExtractor<>(javaType, this) {

            @Override
            @Nullable protected X doExtract(final ResultSet rs, final int paramIndex, final WrapperOptions options)
                    throws SQLException {
                var obj = rs.getObject(paramIndex, BsonObjectId.class);
                var value = obj == null ? null : obj.getValue();
                return javaType.getJavaTypeClass().cast(value);
            }

            @Override
            protected X doExtract(final CallableStatement statement, final int index, final WrapperOptions options)
                    throws SQLException {
                throw new NotSupportedSQLException();
            }

            @Override
            protected X doExtract(final CallableStatement statement, final String name, final WrapperOptions options)
                    throws SQLException {
                throw new NotSupportedSQLException();
            }
        };
    }

    @Override
    public String getFriendlyName() {
        return "OBJECT_ID";
    }

    @Override
    public int getJdbcTypeCode() {
        return SqlTypes.JAVA_OBJECT;
    }

    @Override
    public String toString() {
        return "ObjectIdTypeDescriptor(" + getFriendlyName() + ")";
    }
}
