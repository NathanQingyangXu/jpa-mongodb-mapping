package org.hibernate.omm.type.struct;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.bson.BsonDocument;
import org.hibernate.dialect.StructHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;
import org.hibernate.omm.util.TypeUtil;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.StructJdbcType;

public class MongoSQLStructJdbcType implements StructJdbcType {
    public static final MongoSQLStructJdbcType INSTANCE = new MongoSQLStructJdbcType();

    private String structTypeName;
    private EmbeddableMappingType embeddableMappingType;

    private MongoSQLStructJdbcType() {}

    private MongoSQLStructJdbcType(EmbeddableMappingType embeddableMappingType, String structTypeName) {
        this.embeddableMappingType = embeddableMappingType;
        this.structTypeName = structTypeName;
    }

    @Override
    public MongoSQLStructJdbcType resolveAggregateJdbcType(
            EmbeddableMappingType mappingType, String structTypeName, RuntimeModelCreationContext creationContext) {
        return new MongoSQLStructJdbcType(mappingType, structTypeName);
    }

    @Override
    public EmbeddableMappingType getEmbeddableMappingType() {
        return embeddableMappingType;
    }

    @Override
    public BsonDocument createJdbcValue(Object domainValue, WrapperOptions options) {
        assert embeddableMappingType != null;
        final var document = new BsonDocument();
        final Object[] jdbcValues = embeddableMappingType.getValues(domainValue);
        for (int i = 0; i < jdbcValues.length; i++) {
            document.put(
                    embeddableMappingType.getJdbcValueSelectable(i).getSelectableName(), TypeUtil.wrap(jdbcValues[i]));
        }
        return document;
    }

    @Override
    public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) {
        assert rawJdbcValue instanceof BsonDocument;
        return ((BsonDocument) rawJdbcValue)
                .values().stream().map(TypeUtil::unwrap).toArray();
    }

    @Override
    public int getJdbcTypeCode() {
        return Types.STRUCT;
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
        return new BasicBinder<>(javaType, this) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
                    throws SQLException {
                final var jdbcValue = ((MongoSQLStructJdbcType) getJdbcType()).createJdbcValue(value, options);
                st.setObject(index, jdbcValue, Types.STRUCT);
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
                    throws SQLException {
                throw new NotSupportedSQLException();
            }

            @Override
            public Object getBindValue(X value, WrapperOptions options) throws SQLException {
                return ((MongoSQLStructJdbcType) getJdbcType()).getBindValue(value, options);
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
        return new BasicExtractor<>(javaType, this) {
            @Override
            protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
                return javaType.getJavaTypeClass().cast(rs.getObject(paramIndex));
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                throw new NotSupportedSQLException();
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
                    throws SQLException {
                throw new NotSupportedSQLException();
            }
        };
    }

    @Override
    public String getStructTypeName() {
        return structTypeName;
    }

    private <X> Object getBindValue(X value, WrapperOptions options) throws SQLException {
        return StructHelper.getJdbcValues(embeddableMappingType, null, value, options);
    }
}
