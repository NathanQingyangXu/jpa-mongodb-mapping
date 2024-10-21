package org.hibernate.omm.type.array;

import java.sql.Types;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.spi.TypeConfiguration;

public class MongoSQLArrayJdbcTypeConstructor implements JdbcTypeConstructor {

    public static final MongoSQLArrayJdbcTypeConstructor INSTANCE = new MongoSQLArrayJdbcTypeConstructor();

    @Override
    public JdbcType resolveType(
            TypeConfiguration typeConfiguration,
            Dialect dialect,
            BasicType<?> elementType,
            ColumnTypeInformation columnTypeInformation) {
        return resolveType(typeConfiguration, dialect, elementType.getJdbcType(), columnTypeInformation);
    }

    @Override
    public JdbcType resolveType(
            TypeConfiguration typeConfiguration,
            Dialect dialect,
            JdbcType elementType,
            ColumnTypeInformation columnTypeInformation) {
        return new MongoSQLArrayJdbcType(elementType);
    }

    @Override
    public int getDefaultSqlTypeCode() {
        return Types.ARRAY;
    }
}
