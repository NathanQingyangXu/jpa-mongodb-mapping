package org.hibernate.omm.dialect;

import org.hibernate.engine.jdbc.dialect.spi.BasicDialectResolver;
import org.hibernate.omm.jdbc.MongoDatabaseMetaData;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoDialectResolver extends BasicDialectResolver {

    public MongoDialectResolver() {
        super(MongoDatabaseMetaData.MONGO_DATABASE_PRODUCT_NAME, MongoDialect.class);
    }
}
