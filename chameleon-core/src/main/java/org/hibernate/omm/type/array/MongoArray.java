package org.hibernate.omm.type.array;

import java.util.Map;
import org.hibernate.omm.jdbc.adapter.ArrayAdapter;
import org.hibernate.omm.type.MongoSqlType;

public class MongoArray implements ArrayAdapter {

    private final String baseTypeName;
    private final Object elements;

    private int baseType = MongoSqlType.UNKNOWN;

    public MongoArray(final String baseTypeName, final Object elements) {
        this.baseTypeName = baseTypeName;
        this.elements = elements;
    }

    public void setBaseType(final Integer baseType) {
        this.baseType = baseType;
    }

    @Override
    public int getBaseType() {
        return baseType;
    }

    @Override
    public Object getArray() {
        return elements;
    }

    @Override
    public String getBaseTypeName() {
        return baseTypeName;
    }

    @Override
    public Object getArray(final Map map) {
        return elements;
    }

    @Override
    public Object getArray(final long index, final int count) {
        return elements;
    }

    @Override
    public Object getArray(final long index, final int count, final Map map) {
        return elements;
    }
}
