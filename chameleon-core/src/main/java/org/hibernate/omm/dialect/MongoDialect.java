package org.hibernate.omm.dialect;

import static org.hibernate.type.SqlTypes.ARRAY;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.omm.dialect.exporter.MongoIndexCommandUtil;
import org.hibernate.omm.translate.MongoTranslatorFactory;
import org.hibernate.omm.type.ObjectIdJavaType;
import org.hibernate.omm.type.ObjectIdJdbcType;
import org.hibernate.omm.type.array.MongoSQLArrayJdbcTypeConstructor;
import org.hibernate.omm.type.array.function.MongoArrayContainsFunction;
import org.hibernate.omm.type.array.function.MongoArrayIncludesFunction;
import org.hibernate.omm.type.struct.MongoSQLAggregateSupport;
import org.hibernate.omm.type.struct.MongoSQLStructJdbcType;
import org.hibernate.omm.util.StringUtil;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoDialect extends Dialect {

    public static final int MINIMUM_MONGODB_MAJOR_VERSION_SUPPORTED = 3;

    private static final DatabaseVersion MINIMUM_VERSION =
            DatabaseVersion.make(MINIMUM_MONGODB_MAJOR_VERSION_SUPPORTED);

    private static final class NO_OP_EXPORTER<T extends Exportable> implements Exporter<T> {

        @Override
        public String[] getSqlCreateStrings(
                final T exportable, final Metadata metadata, final SqlStringGenerationContext context) {
            return ArrayHelper.EMPTY_STRING_ARRAY;
        }

        @Override
        public String[] getSqlDropStrings(
                final T exportable, final Metadata metadata, final SqlStringGenerationContext context) {
            return ArrayHelper.EMPTY_STRING_ARRAY;
        }
    }

    public MongoDialect() {
        this(MINIMUM_VERSION);
    }

    public MongoDialect(final DatabaseVersion version) {
        super(version);
    }

    public MongoDialect(final DialectResolutionInfo dialectResolutionInfo) {
        super(dialectResolutionInfo);
    }

    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return new MongoTranslatorFactory();
    }

    @Override
    public void appendLiteral(final SqlAppender appender, final String literal) {
        appender.appendSql(StringUtil.writeStringHelper(literal));
    }

    @Override
    public boolean supportsNullPrecedence() {
        return false;
    }

    @Override
    public boolean supportsStandardArrays() {
        return true;
    }

    @Override
    public int getPreferredSqlTypeCodeForArray() {
        return ARRAY;
    }

    @Override
    public void contribute(final TypeContributions typeContributions, final ServiceRegistry serviceRegistry) {
        super.contribute(typeContributions, serviceRegistry);
        contributeMongoTypes(typeContributions);
    }

    protected void contributeMongoTypes(TypeContributions typeContributions) {
        final var javaTypeRegistry = typeContributions.getTypeConfiguration().getJavaTypeRegistry();
        final var jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

        // Struct
        jdbcTypeRegistry.addDescriptor(MongoSQLStructJdbcType.INSTANCE);

        // array
        jdbcTypeRegistry.addTypeConstructor(MongoSQLArrayJdbcTypeConstructor.INSTANCE);

        // ObjectId
        javaTypeRegistry.addDescriptor(ObjectIdJavaType.getInstance());
        jdbcTypeRegistry.addDescriptor(ObjectIdJdbcType.getInstance());
    }

    @Override
    public void initializeFunctionRegistry(final FunctionContributions functionContributions) {
        var functionRegistry = functionContributions.getFunctionRegistry();
        var typeConfiguration = functionContributions.getTypeConfiguration();
        functionRegistry.register("array_contains", new MongoArrayContainsFunction(typeConfiguration));
        functionRegistry.register("array_includes", new MongoArrayIncludesFunction(typeConfiguration));
    }

    @Override
    public AggregateSupport getAggregateSupport() {
        return MongoSQLAggregateSupport.INSTANCE;
    }

    @Override
    public ParameterMarkerStrategy getNativeParameterMarkerStrategy() {
        return MongoParameterMarkerStrategy.INSTANCE;
    }

    @Override
    public Exporter<Table> getTableExporter() {
        return new NO_OP_EXPORTER<>();
    }

    @Override
    public Exporter<Index> getIndexExporter() {
        return new Exporter<>() {
            @Override
            public String[] getSqlCreateStrings(
                    final Index index, final Metadata metadata, final SqlStringGenerationContext context) {
                final var collectionName = index.getTable().getName();
                final var keys = new BsonDocument();
                for (Selectable selectable : index.getSelectables()) {
                    if (!selectable.isFormula()) {
                        keys.put(selectable.getText(), new BsonInt32(1));
                    }
                }
                return new String[] {
                    MongoIndexCommandUtil.getIndexCreationCommand(collectionName, index.getName(), keys, false)
                            .toJson()
                };
            }

            @Override
            public String[] getSqlDropStrings(
                    final Index index, final Metadata metadata, final SqlStringGenerationContext context) {
                final var collectionName = index.getTable().getName();
                final var keys = MongoIndexCommandUtil.getKeys(index);
                return new String[] {
                    MongoIndexCommandUtil.getIndexDeletionCommand(collectionName, index.getName(), keys)
                            .toJson()
                };
            }
        };
    }

    @Override
    public Exporter<Constraint> getUniqueKeyExporter() {
        return new Exporter<>() {
            @Override
            public String[] getSqlCreateStrings(
                    final Constraint constraint, final Metadata metadata, final SqlStringGenerationContext context) {
                final var collectionName = constraint.getTable().getName();
                final var keys = MongoIndexCommandUtil.getKeys(constraint);
                return new String[] {
                    MongoIndexCommandUtil.getIndexCreationCommand(collectionName, constraint.getName(), keys, true)
                            .toJson()
                };
            }

            @Override
            public String[] getSqlDropStrings(
                    final Constraint constraint, final Metadata metadata, final SqlStringGenerationContext context) {
                final var collectionName = constraint.getTable().getName();
                final var keys = MongoIndexCommandUtil.getKeys(constraint);
                return new String[] {
                    MongoIndexCommandUtil.getIndexDeletionCommand(collectionName, constraint.getName(), keys)
                            .toJson()
                };
            }
        };
    }

    private static class MongoParameterMarkerStrategy implements ParameterMarkerStrategy {
        /**
         * Singleton access
         */
        public static final MongoParameterMarkerStrategy INSTANCE = new MongoParameterMarkerStrategy();

        @Override
        public String createMarker(int position, JdbcType jdbcType) {
            return "{$undefined: true}";
        }
    }
}
