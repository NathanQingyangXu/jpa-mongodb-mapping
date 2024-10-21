package org.hibernate.omm.util;

import com.mongodb.assertions.Assertions;
import com.mongodb.lang.Nullable;
import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.bson.BsonArray;
import org.bson.BsonBinary;
import org.bson.BsonBoolean;
import org.bson.BsonDateTime;
import org.bson.BsonDecimal128;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonTimestamp;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.types.Decimal128;
import org.hibernate.omm.exception.NotSupportedRuntimeException;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public final class TypeUtil {
  private TypeUtil() {}

  public static Integer getJdbcType(final BsonType bsonType) {
    Assertions.notNull("bsonType", bsonType);
    return switch (bsonType) {
      case ARRAY -> Types.ARRAY;
      case BINARY -> Types.BINARY;
      case BOOLEAN -> Types.BOOLEAN;
      case DATE_TIME -> Types.TIME;
      case DECIMAL128 -> Types.DECIMAL;
      case DOUBLE -> Types.DOUBLE;
      case INT32 -> Types.INTEGER;
      case INT64 -> Types.BIGINT;
      case STRING -> Types.VARCHAR;
      case TIMESTAMP -> Types.TIMESTAMP;
      default -> Types.NULL;
    };
  }

  public static BsonValue wrap(@Nullable Object value) {
    if (value == null) {
      return BsonNull.VALUE;
    }
    if (value instanceof BsonValue bsonValue) {
      return bsonValue;
    }
    if (value instanceof Boolean boolValue) {
      return BsonBoolean.valueOf(boolValue);
    }
    if (value instanceof Float floatValue) {
      return new BsonDouble(floatValue);
    }
    if (value instanceof Double doubleValue) {
      return new BsonDouble(doubleValue);
    }
    if (value instanceof Byte byteValue) {
      return new BsonInt32(byteValue);
    }
    if (value instanceof Short shortValue) {
      return new BsonInt32(shortValue);
    }
    if (value instanceof Integer intValue) {
      return new BsonInt32(intValue);
    }
    if (value instanceof Long longValue) {
      return new BsonInt64(longValue);
    }
    if (value instanceof BigDecimal bigDecimalValue) {
      return new BsonDecimal128(new Decimal128(bigDecimalValue));
    }
    if (value instanceof String stringValue) {
      return new BsonString(stringValue);
    }
    if (value instanceof byte[] bytesValue) {
      return new BsonBinary(bytesValue);
    }
    if (value instanceof Date dateValue) {
      return new BsonDateTime(dateValue.toInstant().toEpochMilli());
    }
    if (value.getClass().isArray() || Iterable.class.isAssignableFrom(value.getClass())) {
      final var iterable =
          value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Iterable<?>) value;
      final var iterator = iterable.iterator();

      final List<BsonValue> elements = new ArrayList<>();
      while (iterator.hasNext()) {
        elements.add(wrap(iterator.next()));
      }
      return new BsonArray(elements);
    }
    throw new NotSupportedRuntimeException("unknown JDBC type: " + value.getClass());
  }

  @Nullable
  public static Object unwrap(@Nullable final BsonValue bsonValue) {
    if (bsonValue == null) {
      return null;
    }
    return switch (bsonValue.getBsonType()) {
      case ARRAY -> ((BsonArray) bsonValue).getValues().stream().map(TypeUtil::unwrap).toList();
      case BINARY -> ((BsonBinary) bsonValue).getData();
      case BOOLEAN -> ((BsonBoolean) bsonValue).getValue();
      case DATE_TIME -> ((BsonDateTime) bsonValue).getValue();
      case DECIMAL128 -> ((BsonDecimal128) bsonValue).getValue().bigDecimalValue();
      case DOUBLE -> ((BsonDouble) bsonValue).getValue();
      case INT32 -> ((BsonInt32) bsonValue).getValue();
      case INT64 -> ((BsonInt64) bsonValue).getValue();
      case STRING -> ((BsonString) bsonValue).getValue();
      case TIMESTAMP -> ((BsonTimestamp) bsonValue).getValue();
      case DOCUMENT -> bsonValue;
      default -> null;
    };
  }
}
