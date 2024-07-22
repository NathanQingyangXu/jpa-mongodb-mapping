/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.omm.dialect;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.SelectItemReferenceStrategy;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.AbstractDelegatingWrapperOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.internal.util.MathHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.omm.exception.NotSupportedRuntimeException;
import org.hibernate.omm.util.CollectionUtil;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.SortDirection;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.FrameExclusion;
import org.hibernate.query.sqm.FrameKind;
import org.hibernate.query.sqm.FrameMode;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.MultipatternSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.AggregateFunctionChecker;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.AggregateColumnWriteExpression;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Collation;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.LiteralAsParameter;
import org.hibernate.sql.ast.tree.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.tree.expression.OrderedSetAggregateFunctionExpression;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.Overflow;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableGroup;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.from.VirtualTableGroup;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate.FilterFragmentParameter;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate.FilterFragmentPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InArrayPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.predicate.ThruthnessPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.AbstractJdbcParameter;
import org.hibernate.sql.exec.internal.JdbcOperationQueryInsertImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcLockStrategy;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQueryDelete;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcOperationQueryUpdate;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.internal.TableDeleteCustomSql;
import org.hibernate.sql.model.internal.TableDeleteStandard;
import org.hibernate.sql.model.internal.TableInsertCustomSql;
import org.hibernate.sql.model.internal.TableInsertStandard;
import org.hibernate.sql.model.internal.TableUpdateCustomSql;
import org.hibernate.sql.model.internal.TableUpdateStandard;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hibernate.query.sqm.TemporalUnit.NANOSECOND;
import static org.hibernate.sql.ast.SqlTreePrinter.logSqlAst;
import static org.hibernate.sql.results.graph.DomainResultGraphPrinter.logDomainResultGraph;

/**
 * @author Nathan Xu
 */
public class MongoJsonAstTranslator<T extends JdbcOperation> implements SqlAstTranslator<T>, SqlAppender {

    // pre-req state
    private final SessionFactoryImplementor sessionFactory;

    // In-flight state
    private final StringBuilder sqlBuffer = new StringBuilder();

    private final List<JdbcParameterBinder> parameterBinders = new ArrayList<>();
    private final JdbcParametersImpl jdbcParameters = new JdbcParametersImpl();
    private JdbcParameterBindings jdbcParameterBindings;
    private Map<JdbcParameter, JdbcParameterBinding> appliedParameterBindings = Collections.emptyMap();
    private SqlAstNodeRenderingMode parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
    private final ParameterMarkerStrategy parameterMarkerStrategy;


    private final Stack<Clause> clauseStack = new StandardStack<>(Clause.class);
    private final Stack<QueryPart> queryPartStack = new StandardStack<>(QueryPart.class);
    private final Stack<Statement> statementStack = new StandardStack<>(Statement.class);

    private final Dialect dialect;
    private final Set<String> affectedTableNames = new HashSet<>();
    private boolean needsSelectAliases;
    // Column aliases that need to be injected
    private List<String> columnAliases;
    private Predicate additionalWherePredicate;
    // We must reset the queryPartForRowNumbering fields to null if a query part is visited that does not
    // contribute to the row numbering i.e. if the query part is a sub-query in the where clause.
    // To determine whether a query part contributes to row numbering, we remember the clause depth
    // and when visiting a query part, compare the current clause depth against the remembered one.
    private QueryPart queryPartForRowNumbering;
    private int queryPartForRowNumberingClauseDepth = -1;
    private int queryPartForRowNumberingAliasCounter;
    private int queryGroupAliasCounter;

    private transient AbstractSqmSelfRenderingFunctionDescriptor castFunction;
    private transient LazySessionWrapperOptions lazySessionWrapperOptions;
    private transient BasicType<Integer> integerType;
    private transient BasicType<String> stringType;
    private transient BasicType<Boolean> booleanType;

    private LockOptions lockOptions;
    private Limit limit;
    private JdbcParameter offsetParameter;
    private JdbcParameter limitParameter;
    private ForUpdateClause forUpdate;

    public MongoJsonAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
        this.sessionFactory = sessionFactory;
        final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
        this.dialect = jdbcServices.getDialect();
        this.statementStack.push(statement);
        this.parameterMarkerStrategy = jdbcServices.getParameterMarkerStrategy();
    }

    @Override
    public SessionFactoryImplementor getSessionFactory() {
        return sessionFactory;
    }

    protected AbstractSqmSelfRenderingFunctionDescriptor castFunction() {
        if (castFunction == null) {
            castFunction = findSelfRenderingFunction("cast", 2);
        }
        return castFunction;
    }

    protected WrapperOptions getWrapperOptions() {
        if (lazySessionWrapperOptions == null) {
            lazySessionWrapperOptions = new LazySessionWrapperOptions(sessionFactory);
        }
        return lazySessionWrapperOptions;
    }

    public BasicType<Integer> getIntegerType() {
        if (integerType == null) {
            integerType = sessionFactory.getTypeConfiguration()
                    .getBasicTypeRegistry()
                    .resolve(StandardBasicTypes.INTEGER);
        }
        return integerType;
    }

    public BasicType<String> getStringType() {
        if (stringType == null) {
            stringType = sessionFactory.getTypeConfiguration()
                    .getBasicTypeRegistry()
                    .resolve(StandardBasicTypes.STRING);
        }
        return stringType;
    }

    public BasicType<Boolean> getBooleanType() {
        if (booleanType == null) {
            booleanType = sessionFactory.getTypeConfiguration()
                    .getBasicTypeRegistry()
                    .resolve(StandardBasicTypes.BOOLEAN);
        }
        return booleanType;
    }

    /**
     * A lazy session implementation that is needed for rendering literals.
     * Usually, only the {@link WrapperOptions} interface is needed,
     * but for creating LOBs, it might be to have a full-blown session.
     */
    private static class LazySessionWrapperOptions extends AbstractDelegatingWrapperOptions {

        private final SessionFactoryImplementor sessionFactory;
        private SessionImplementor session;

        public LazySessionWrapperOptions(SessionFactoryImplementor sessionFactory) {
            this.sessionFactory = sessionFactory;
        }

        public void cleanup() {
            if (session != null) {
                session.close();
                session = null;
            }
        }

        @Override
        protected SessionImplementor delegate() {
            if (session == null) {
                session = sessionFactory.openTemporarySession();
            }
            return session;
        }

        @Override
        public SharedSessionContractImplementor getSession() {
            return delegate();
        }

        @Override
        public SessionFactoryImplementor getSessionFactory() {
            return sessionFactory;
        }

        @Override
        public boolean useStreamForLobBinding() {
            return sessionFactory.getFastSessionServices().useStreamForLobBinding();
        }

        @Override
        public int getPreferredSqlTypeCodeForBoolean() {
            return sessionFactory.getFastSessionServices().getPreferredSqlTypeCodeForBoolean();
        }

        @Override
        public TimeZone getJdbcTimeZone() {
            return sessionFactory.getSessionFactoryOptions().getJdbcTimeZone();
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public String getSql() {
        return sqlBuffer.toString();
    }

    protected void cleanup() {
        if (lazySessionWrapperOptions != null) {
            lazySessionWrapperOptions.cleanup();
            lazySessionWrapperOptions = null;
        }
        this.jdbcParameterBindings = null;
        this.lockOptions = null;
        this.limit = null;
        setOffsetParameter(null);
        setLimitParameter(null);
    }

    public List<JdbcParameterBinder> getParameterBinders() {
        return parameterBinders;
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @SuppressWarnings("unused")
    protected SqlAppender getSqlAppender() {
        return this;
    }

    @Override
    public Set<String> getAffectedTableNames() {
        return affectedTableNames;
    }

    protected Statement getStatement() {
        return statementStack.getRoot();
    }

    public MutationStatement getCurrentDmlStatement() {
        return statementStack.findCurrentFirst(MongoJsonAstTranslator::matchMutationStatement);
    }

    private static MutationStatement matchMutationStatement(Statement stmt) {
        if (stmt instanceof MutationStatement) {
            return (MutationStatement) stmt;
        }
        return null;
    }

    protected SqlAstNodeRenderingMode getParameterRenderingMode() {
        return parameterRenderingMode;
    }

    protected void addAdditionalWherePredicate(Predicate predicate) {
        additionalWherePredicate = Predicate.combinePredicates(additionalWherePredicate, predicate);
    }

    @Override
    public boolean supportsFilterClause() {
        // By default, we report false because not many dialects support this
        return false;
    }

    @Override
    public void appendSql(String fragment) {
        sqlBuffer.append(fragment);
    }

    @Override
    public void appendSql(char fragment) {
        sqlBuffer.append(fragment);
    }

    @Override
    public void appendSql(int value) {
        sqlBuffer.append(value);
    }

    @Override
    public void appendSql(long value) {
        sqlBuffer.append(value);
    }

    @Override
    public void appendSql(boolean value) {
        sqlBuffer.append(value);
    }

    @Override
    public Appendable append(CharSequence csq) {
        sqlBuffer.append(csq);
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) {
        sqlBuffer.append(csq, start, end);
        return this;
    }

    @Override
    public Appendable append(char c) {
        sqlBuffer.append(c);
        return this;
    }

    protected void addAppliedParameterBinding(JdbcParameter parameter, JdbcParameterBinding binding) {
        if (appliedParameterBindings.isEmpty()) {
            appliedParameterBindings = new IdentityHashMap<>();
        }
        if (binding == null) {
            appliedParameterBindings.put(parameter, null);
        } else {
            final JdbcMapping bindType = binding.getBindType();
            //noinspection unchecked
            final Object value = ((JavaType<Object>) bindType.getJdbcJavaType())
                    .getMutabilityPlan()
                    .deepCopy(binding.getBindValue());
            appliedParameterBindings.put(parameter, new JdbcParameterBindingImpl(bindType, value));
        }
    }

    protected Map<JdbcParameter, JdbcParameterBinding> getAppliedParameterBindings() {
        return appliedParameterBindings;
    }

    protected JdbcLockStrategy getJdbcLockStrategy() {
        return lockOptions == null ? JdbcLockStrategy.FOLLOW_ON : JdbcLockStrategy.NONE;
    }

    protected JdbcParameterBindings getJdbcParameterBindings() {
        return jdbcParameterBindings;
    }

    protected LockOptions getLockOptions() {
        return lockOptions;
    }

    protected Limit getLimit() {
        return limit;
    }

    protected boolean hasLimit() {
        return limit != null && !limit.isEmpty();
    }

    protected JdbcParameter getOffsetParameter() {
        return offsetParameter;
    }

    protected void setOffsetParameter(JdbcParameter offsetParameter) {
        this.offsetParameter = offsetParameter;
    }

    protected JdbcParameter getLimitParameter() {
        return limitParameter;
    }

    protected void setLimitParameter(JdbcParameter limitParameter) {
        this.limitParameter = limitParameter;
    }

    @SuppressWarnings("unchecked")
    protected <R> R interpretExpression(Expression expression, JdbcParameterBindings jdbcParameterBindings) {
        if (expression instanceof Literal) {
            return (R) ((Literal) expression).getLiteralValue();
        } else if (expression instanceof JdbcParameter) {
            if (jdbcParameterBindings == null) {
                throw new IllegalArgumentException(
                        "Can't interpret expression because no parameter bindings are available");
            }
            return (R) getParameterBindValue((JdbcParameter) expression);
        } else if (expression instanceof SqmParameterInterpretation) {
            if (jdbcParameterBindings == null) {
                throw new IllegalArgumentException(
                        "Can't interpret expression because no parameter bindings are available");
            }
            return (R) getParameterBindValue((JdbcParameter) ((SqmParameterInterpretation) expression).getResolvedExpression());
        }
        throw new UnsupportedOperationException("Can't interpret expression: " + expression);
    }

    protected void renderExpressionAsLiteral(Expression expression, JdbcParameterBindings jdbcParameterBindings) {
        if (expression instanceof Literal) {
            expression.accept(this);
            return;
        } else if (expression instanceof JdbcParameter) {
            if (jdbcParameterBindings == null) {
                throw new IllegalArgumentException(
                        "Can't interpret expression because no parameter bindings are available");
            }
            final JdbcParameter parameter = (JdbcParameter) expression;
            renderAsLiteral(parameter, getParameterBindValue(parameter));
            return;
        } else if (expression instanceof SqmParameterInterpretation) {
            if (jdbcParameterBindings == null) {
                throw new IllegalArgumentException(
                        "Can't interpret expression because no parameter bindings are available");
            }
            final JdbcParameter parameter = (JdbcParameter) ((SqmParameterInterpretation) expression).getResolvedExpression();
            renderAsLiteral(parameter, getParameterBindValue(parameter));
            return;
        }
        throw new UnsupportedOperationException("Can't render expression as literal: " + expression);
    }

    protected Object getParameterBindValue(JdbcParameter parameter) {
        final JdbcParameterBinding binding;
        if (parameter == getOffsetParameter()) {
            binding = new JdbcParameterBindingImpl(getIntegerType(), getLimit().getFirstRow());
        } else if (parameter == getLimitParameter()) {
            binding = new JdbcParameterBindingImpl(getIntegerType(), getLimit().getMaxRows());
        } else {
            binding = jdbcParameterBindings.getBinding(parameter);
        }
        addAppliedParameterBinding(parameter, binding);
        return binding.getBindValue();
    }

    protected Expression getLeftHandExpression(Predicate predicate) {
        if (predicate instanceof NullnessPredicate) {
            return ((NullnessPredicate) predicate).getExpression();
        }
        assert predicate instanceof ComparisonPredicate;
        return ((ComparisonPredicate) predicate).getLeftHandExpression();
    }

    protected boolean inOverOrWithinGroupClause() {
        return clauseStack.findCurrentFirst(MongoJsonAstTranslator::matchOverOrWithinGroupClauses) != null;
    }

    private static Boolean matchOverOrWithinGroupClauses(final Clause clause) {
        switch (clause) {
            case OVER:
            case WITHIN_GROUP:
                return Boolean.TRUE;
            default:
                return null;
        }
    }

    protected Stack<Clause> getClauseStack() {
        return clauseStack;
    }

    protected Stack<Statement> getStatementStack() {
        return statementStack;
    }

    protected Stack<QueryPart> getQueryPartStack() {
        return queryPartStack;
    }

    @Override
    public QueryPart getCurrentQueryPart() {
        return queryPartStack.getCurrent();
    }

    @Override
    public Stack<Clause> getCurrentClauseStack() {
        return clauseStack;
    }

    @Override
    public T translate(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
        try {
            this.jdbcParameterBindings = jdbcParameterBindings;

            final Statement statement = statementStack.pop();

            if (statement instanceof TableMutation) {
                return translateTableMutation((TableMutation<?>) statement);
            }

            this.lockOptions = queryOptions.getLockOptions().makeCopy();
            this.limit = queryOptions.getLimit() == null ? null : queryOptions.getLimit().makeCopy();
            final JdbcOperation jdbcOperation;
            if (statement instanceof DeleteStatement) {
                jdbcOperation = translateDelete((DeleteStatement) statement);
            } else if (statement instanceof UpdateStatement) {
                jdbcOperation = translateUpdate((UpdateStatement) statement);
            } else if (statement instanceof InsertSelectStatement) {
                jdbcOperation = translateInsert((InsertSelectStatement) statement);
            } else if (statement instanceof SelectStatement) {
                jdbcOperation = translateSelect((SelectStatement) statement);
            } else {
                throw new IllegalArgumentException("Unexpected statement - " + statement);
            }

            //noinspection unchecked
            return (T) jdbcOperation;
        } finally {
            cleanup();
        }
    }

    protected JdbcOperationQueryDelete translateDelete(DeleteStatement sqlAst) {
        visitDeleteStatement(sqlAst);

        return new JdbcOperationQueryDelete(
                getSql(),
                getParameterBinders(),
                getAffectedTableNames(),
                getAppliedParameterBindings()
        );
    }

    protected JdbcOperationQueryUpdate translateUpdate(UpdateStatement sqlAst) {
        visitUpdateStatement(sqlAst);

        return new JdbcOperationQueryUpdate(
                getSql(),
                getParameterBinders(),
                getAffectedTableNames(),
                getAppliedParameterBindings()
        );
    }

    protected JdbcOperationQueryInsert translateInsert(InsertSelectStatement sqlAst) {
        visitInsertStatement(sqlAst);

        return new JdbcOperationQueryInsertImpl(
                getSql(),
                getParameterBinders(),
                getAffectedTableNames(),
                getUniqueConstraintNameThatMayFail(sqlAst)
        );
    }

    protected String getUniqueConstraintNameThatMayFail(InsertSelectStatement sqlAst) {
        final ConflictClause conflictClause = sqlAst.getConflictClause();
        if (conflictClause == null || !conflictClause.getConstraintColumnNames().isEmpty()) {
            return null;
        } else {
            if (sqlAst.getSourceSelectStatement() != null && !isFetchFirstRowOnly(sqlAst.getSourceSelectStatement())
                    || sqlAst.getValuesList().size() > 1) {
                throw new IllegalQueryOperationException(
                        "Can't emulate conflict clause with constraint name for more than one row to insert");
            }
            return conflictClause.getConstraintName() == null ? "" : conflictClause.getConstraintName();
        }
    }

    protected JdbcOperationQuerySelect translateSelect(SelectStatement selectStatement) {
        logDomainResultGraph(selectStatement.getDomainResultDescriptors());
        logSqlAst(selectStatement);

        visitSelectStatement(selectStatement);

        final int rowsToSkip;
        return new JdbcOperationQuerySelect(
                getSql(),
                getParameterBinders(),
                buildJdbcValuesMappingProducer(selectStatement),
                getAffectedTableNames(),
                rowsToSkip = getRowsToSkip(selectStatement, getJdbcParameterBindings()),
                getMaxRows(selectStatement, getJdbcParameterBindings(), rowsToSkip),
                getAppliedParameterBindings(),
                getJdbcLockStrategy(),
                getOffsetParameter(),
                getLimitParameter()
        );
    }

    private JdbcValuesMappingProducer buildJdbcValuesMappingProducer(SelectStatement selectStatement) {
        return getSessionFactory()
                .getFastSessionServices()
                .getJdbcValuesMappingProducerProvider()
                .buildMappingProducer(selectStatement, getSessionFactory());
    }

    protected int getRowsToSkip(SelectStatement sqlAstSelect, JdbcParameterBindings jdbcParameterBindings) {
        if (hasLimit()) {
            if (offsetParameter != null && needsRowsToSkip()) {
                return interpretExpression(offsetParameter, jdbcParameterBindings);
            }
        } else {
            final Expression offsetClauseExpression = sqlAstSelect.getQueryPart().getOffsetClauseExpression();
            if (offsetClauseExpression != null && needsRowsToSkip()) {
                return interpretExpression(offsetClauseExpression, jdbcParameterBindings);
            }
        }
        return 0;
    }

    protected int getMaxRows(
            SelectStatement sqlAstSelect,
            JdbcParameterBindings jdbcParameterBindings,
            int rowsToSkip) {
        if (hasLimit()) {
            if (limitParameter != null && needsMaxRows()) {
                final Number fetchCount = interpretExpression(limitParameter, jdbcParameterBindings);
                return rowsToSkip + fetchCount.intValue();
            }
        } else {
            final Expression fetchClauseExpression = sqlAstSelect.getQueryPart().getFetchClauseExpression();
            if (fetchClauseExpression != null && needsMaxRows()) {
                final Number fetchCount = interpretExpression(fetchClauseExpression, jdbcParameterBindings);
                return rowsToSkip + fetchCount.intValue();
            }
        }
        return Integer.MAX_VALUE;
    }

    protected boolean needsRowsToSkip() {
        return false;
    }

    protected boolean needsMaxRows() {
        return false;
    }

    protected void prepareLimitOffsetParameters() {
        final Limit limit = getLimit();
        if (limit.getFirstRow() != null) {
            setOffsetParameter(
                    new OffsetJdbcParameter(
                            sessionFactory.getTypeConfiguration().getBasicTypeForJavaType(Integer.class)
                    )
            );
        }
        if (limit.getMaxRows() != null) {
            setLimitParameter(
                    new LimitJdbcParameter(
                            sessionFactory.getTypeConfiguration().getBasicTypeForJavaType(Integer.class)
                    )
            );
        }
    }

    private static class OffsetJdbcParameter extends AbstractJdbcParameter {

        public OffsetJdbcParameter(BasicType<Integer> type) {
            super(type);
        }

        @Override
        public void bindParameterValue(
                PreparedStatement statement,
                int startPosition,
                JdbcParameterBindings jdbcParamBindings,
                ExecutionContext executionContext) throws SQLException {
            //noinspection unchecked
            getJdbcMapping().getJdbcValueBinder().bind(
                    statement,
                    executionContext.getQueryOptions().getLimit().getFirstRow(),
                    startPosition,
                    executionContext.getSession()
            );
        }
    }

    private static class LimitJdbcParameter extends AbstractJdbcParameter {

        public LimitJdbcParameter(BasicType<Integer> type) {
            super(type);
        }

        @Override
        public void bindParameterValue(
                PreparedStatement statement,
                int startPosition,
                JdbcParameterBindings jdbcParamBindings,
                ExecutionContext executionContext) throws SQLException {
            //noinspection unchecked
            getJdbcMapping().getJdbcValueBinder().bind(
                    statement,
                    executionContext.getQueryOptions().getLimit().getMaxRows(),
                    startPosition,
                    executionContext.getSession()
            );
        }
    }

    @Override
    public void visitSelectStatement(SelectStatement statement) {
        try {
            statementStack.push(statement);
            parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
            if (!statement.getQueryPart().isRoot()) {
                throw new NotSupportedRuntimeException("subQuery not supported");
            }
            statement.getQueryPart().accept(this);
        } finally {
            statementStack.pop();
        }
    }

    @Override
    public void visitDeleteStatement(DeleteStatement statement) {
        try {
            statementStack.push(statement);
            visitDeleteStatementOnly(statement);
        } finally {
            statementStack.pop();
        }
    }

    @Override
    public void visitUpdateStatement(UpdateStatement statement) {
        try {
            statementStack.push(statement);
            visitUpdateStatementOnly(statement);
        } finally {
            statementStack.pop();
        }
    }

    @Override
    public void visitAssignment(Assignment assignment) {
        throw new SqlTreeCreationException("Encountered unexpected assignment clause");
    }

    @Override
    public void visitInsertStatement(InsertSelectStatement statement) {
        try {
            statementStack.push(statement);
            visitInsertStatementOnly(statement);
        } finally {
            statementStack.pop();
        }
    }

    protected void visitDeleteStatementOnly(DeleteStatement statement) {
        renderDeleteClause(statement);
        appendSql(", deletes: [ { q: ");
        if (supportsJoinsInDelete() || !hasNonTrivialFromClause(statement.getFromClause())) {
            visitWhereClause(statement.getRestriction());
        } else {
            visitWhereClause(determineWhereClauseRestrictionWithJoinEmulation(statement));
        }
        //visitReturningColumns( statement.getReturningColumns() );
        appendSql(", limit: 0 } ] }");
    }

    protected boolean supportsJoinsInDelete() {
        return false;
    }

    protected void renderDeleteClause(DeleteStatement statement) {
        appendSql("{ delete: ");
        final Stack<Clause> clauseStack = getClauseStack();
        try {
            clauseStack.push(Clause.DELETE);
            renderDmlTargetTableExpression(statement.getTargetTable());
        } finally {
            clauseStack.pop();
        }
    }

    protected void visitUpdateStatementOnly(UpdateStatement statement) {
        appendSql("{ ");
        renderUpdateClause(statement);
        appendSql(", updates: [ ");
        visitWhereClause(statement.getRestriction());
        renderSetClause(statement.getAssignments());
        appendSql(" ] }");
    }

    protected void renderUpdateClause(UpdateStatement updateStatement) {
        appendSql("update: ");
        final Stack<Clause> clauseStack = getClauseStack();
        try {
            clauseStack.push(Clause.UPDATE);
            renderDmlTargetTableExpression(updateStatement.getTargetTable());
        } finally {
            clauseStack.pop();
        }
    }

    protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
        dialect.appendLiteral(this, tableReference.getTableExpression());
        registerAffectedTable(tableReference);
    }

    protected static boolean hasNonTrivialFromClause(FromClause fromClause) {
        return fromClause != null && !fromClause.getRoots().isEmpty()
                && (fromClause.getRoots().size() > 1 || fromClause.getRoots().get(0).hasRealJoins());
    }

    protected Predicate determineWhereClauseRestrictionWithJoinEmulation(AbstractUpdateOrDeleteStatement statement) {
        return determineWhereClauseRestrictionWithJoinEmulation(statement, null);
    }

    protected Predicate determineWhereClauseRestrictionWithJoinEmulation(
            AbstractUpdateOrDeleteStatement statement,
            String dmlTargetAlias) {
        final QuerySpec querySpec = new QuerySpec(false);
        querySpec.getSelectClause().addSqlSelection(
                new SqlSelectionImpl(new QueryLiteral<>(1, getIntegerType()))
        );
        querySpec.applyPredicate(statement.getRestriction());

        if (supportsJoinInMutationStatementSubquery()) {
            for (TableGroup root : statement.getFromClause().getRoots()) {
                if (root.getPrimaryTableReference() == statement.getTargetTable()) {
                    final TableGroup dmlTargetTableGroup = new StandardTableGroup(
                            true,
                            new NavigablePath("dual"),
                            null,
                            null,
                            new NamedTableReference(getDual(), "d_"),
                            null,
                            sessionFactory
                    );
                    querySpec.getFromClause().addRoot(dmlTargetTableGroup);
                    dmlTargetTableGroup.getTableReferenceJoins().addAll(root.getTableReferenceJoins());
                    for (TableGroupJoin tableGroupJoin : root.getTableGroupJoins()) {
                        dmlTargetTableGroup.addTableGroupJoin(tableGroupJoin);
                    }
                    for (TableGroupJoin tableGroupJoin : root.getNestedTableGroupJoins()) {
                        dmlTargetTableGroup.addNestedTableGroupJoin(tableGroupJoin);
                    }
                } else {
                    querySpec.getFromClause().addRoot(root);
                }
            }
        } else {
            assert dmlTargetAlias != null;
            final TableGroup dmlTargetTableGroup = statement.getFromClause().getRoots().get(0);
            assert dmlTargetTableGroup.getPrimaryTableReference() == statement.getTargetTable();
            for (TableGroup root : statement.getFromClause().getRoots()) {
                querySpec.getFromClause().addRoot(root);
            }
            querySpec.applyPredicate(
                    createRowMatchingPredicate(
                            dmlTargetTableGroup,
                            dmlTargetAlias,
                            dmlTargetTableGroup.getPrimaryTableReference().getIdentificationVariable()
                    )
            );
        }

        return new ExistsPredicate(querySpec, false, getBooleanType());
    }

    protected void renderSetClause(List<Assignment> assignments) {
        appendSql(" , u: { ");
        String separator = " ";
        try {
            clauseStack.push(Clause.SET);
            for (Assignment assignment : assignments) {
                appendSql(separator);
                separator = ", ";
                visitSetAssignment(assignment);
            }
        } finally {
            appendSql(" }");
            clauseStack.pop();
        }
    }

    protected void visitSetAssignment(Assignment assignment) {
        final List<ColumnReference> columnReferences = assignment.getAssignable().getColumnReferences();
        if (columnReferences.size() == 1) {
            columnReferences.get(0).appendColumnForWrite(this, null);
            appendSql(": ");
            final Expression assignedValue = assignment.getAssignedValue();
            final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(assignedValue);
            if (sqlTuple != null) {
                assert sqlTuple.getExpressions().size() == 1;
                sqlTuple.getExpressions().get(0).accept(this);
            } else {
                assignedValue.accept(this);
            }
        } else {
            throw new NotSupportedRuntimeException("multiple column assignment not supported");
        }
    }

    protected void visitInsertStatementOnly(InsertSelectStatement statement) {
        clauseStack.push(Clause.INSERT);

        String tableExpression = statement.getTargetTable().getTableExpression();
        appendSql("{ insert: '");
        appendSql(tableExpression);
        appendSql('\'');
        appendSql(", documents: ");

        boolean firstPass = true;

        registerAffectedTable(tableExpression);

        final List<ColumnReference> targetColumnReferences = statement.getTargetColumns();
        if (targetColumnReferences != null) {
            for (ColumnReference targetColumnReference : targetColumnReferences) {
                if (firstPass) {
                    firstPass = false;
                } else {
                    appendSql(COMMA_SEPARATOR_CHAR);
                }
                appendSql(targetColumnReference.getColumnExpression());
                appendSql(": ?");
            }

        }

        appendSql("}");

        if (statement.getSourceSelectStatement() != null) {
            statement.getSourceSelectStatement().accept(this);
        } else {
            visitInsertValuesList(statement);
        }
        getClauseStack().pop();

    }

    private void visitInsertValuesList(InsertSelectStatement statement) {
        List<ColumnReference> targetColumnReferences = statement.getTargetColumns();
        List<Values> valuesList = statement.getValuesList();
        appendSql('[');
        final Stack<Clause> clauseStack = getClauseStack();
        try {
            clauseStack.push(Clause.VALUES);
            for (int i = 0; i < valuesList.size(); i++) {
                if (i != 0) {
                    appendSql(COMMA_SEPARATOR_CHAR);
                }
                appendSql(" {");
                final List<Expression> expressions = valuesList.get(i).getExpressions();
                for (int j = 0; j < expressions.size(); j++) {
                    if (j != 0) {
                        appendSql(COMMA_SEPARATOR_CHAR);
                    }
                    appendSql(targetColumnReferences.get(j).getColumnExpression());
                    appendSql(": ");
                    expressions.get(j).accept(this);
                }
                appendSql('}');
            }
        } finally {
            appendSql(']');
            clauseStack.pop();
        }
    }

    private QueryPartTableReference updateSourceAsSubquery(UpdateStatement statement, boolean correlated) {
        final QuerySpec inlineView = new QuerySpec(!correlated);
        final SelectClause selectClause = inlineView.getSelectClause();
        final List<Assignment> assignments = statement.getAssignments();
        final List<String> columnNames = new ArrayList<>(assignments.size());
        for (Assignment assignment : assignments) {
            final List<ColumnReference> columnReferences = assignment.getAssignable().getColumnReferences();
            final Expression assignedValue = assignment.getAssignedValue();
            if (columnReferences.size() == 1) {
                selectClause.addSqlSelection(new SqlSelectionImpl(assignedValue));
                columnNames.add("c" + columnNames.size());
            } else if (assignedValue instanceof SqlTuple) {
                final List<? extends Expression> expressions = ((SqlTuple) assignedValue).getExpressions();
                for (int i = 0; i < columnReferences.size(); i++) {
                    selectClause.addSqlSelection(new SqlSelectionImpl(expressions.get(i)));
                    columnNames.add("c" + columnNames.size());
                }
            } else {
                throw new IllegalQueryOperationException("Unsupported tuple assignment in update query with joins.");
            }
        }
        if (!correlated) {
            final String rowIdExpression = dialect.rowId(null);
            if (rowIdExpression == null) {
                final TableGroup dmlTargetTableGroup = statement.getFromClause().getRoots().get(0);
                assert dmlTargetTableGroup.getPrimaryTableReference() == statement.getTargetTable();
                final EntityIdentifierMapping identifierMapping = dmlTargetTableGroup.getModelPart()
                        .asEntityMappingType()
                        .getIdentifierMapping();
                identifierMapping.forEachSelectable(
                        0,
                        (selectionIndex, selectableMapping) -> {
                            selectClause.addSqlSelection(new SqlSelectionImpl(
                                    new ColumnReference(statement.getTargetTable(), selectableMapping)
                            ));
                            columnNames.add(selectableMapping.getSelectionExpression());
                        }
                );
            } else {
                selectClause.addSqlSelection(new SqlSelectionImpl(
                        new ColumnReference(
                                statement.getTargetTable(),
                                rowIdExpression,
                                sessionFactory.getTypeConfiguration().getBasicTypeRegistry()
                                        .resolve(Object.class, dialect.rowIdSqlType())
                        )
                ));
                columnNames.add("c" + columnNames.size());
            }
        }

        if (correlated) {
            for (TableGroup root : statement.getFromClause().getRoots()) {
                if (statement.getTargetTable() == root.getPrimaryTableReference()) {
                    final TableGroup dmlTargetTableGroup = new StandardTableGroup(
                            true,
                            new NavigablePath("dual"),
                            null,
                            null,
                            new NamedTableReference(getDual(), "d_"),
                            null,
                            sessionFactory
                    );
                    inlineView.getFromClause().addRoot(dmlTargetTableGroup);
                    dmlTargetTableGroup.getTableReferenceJoins().addAll(root.getTableReferenceJoins());
                    for (TableGroupJoin tableGroupJoin : root.getTableGroupJoins()) {
                        dmlTargetTableGroup.addTableGroupJoin(tableGroupJoin);
                    }
                    for (TableGroupJoin tableGroupJoin : root.getNestedTableGroupJoins()) {
                        dmlTargetTableGroup.addNestedTableGroupJoin(tableGroupJoin);
                    }
                } else {
                    inlineView.getFromClause().addRoot(root);
                }
            }
        } else {
            for (TableGroup root : statement.getFromClause().getRoots()) {
                inlineView.getFromClause().addRoot(root);
            }
        }
        inlineView.applyPredicate(statement.getRestriction());

        return new QueryPartTableReference(
                new SelectStatement(inlineView),
                "s",
                columnNames,
                false,
                getSessionFactory()
        );
    }

    private QueryPartTableReference updateSourceAsInlineView(UpdateStatement statement) {
        final QuerySpec inlineView = new QuerySpec(true);
        final SelectClause selectClause = inlineView.getSelectClause();
        final List<Assignment> assignments = statement.getAssignments();
        final List<String> columnNames = new ArrayList<>(assignments.size());
        for (Assignment assignment : assignments) {
            final List<ColumnReference> columnReferences = assignment.getAssignable().getColumnReferences();
            final Expression assignedValue = assignment.getAssignedValue();
            if (columnReferences.size() == 1) {
                selectClause.addSqlSelection(new SqlSelectionImpl(columnReferences.get(0)));
                selectClause.addSqlSelection(new SqlSelectionImpl(assignedValue));
                columnNames.add("c" + columnNames.size());
                columnNames.add("c" + columnNames.size());
            } else if (assignedValue instanceof SqlTuple) {
                final List<? extends Expression> expressions = ((SqlTuple) assignedValue).getExpressions();
                for (int i = 0; i < columnReferences.size(); i++) {
                    selectClause.addSqlSelection(new SqlSelectionImpl(columnReferences.get(i)));
                    selectClause.addSqlSelection(new SqlSelectionImpl(expressions.get(i)));
                    columnNames.add("c" + columnNames.size());
                    columnNames.add("c" + columnNames.size());
                }
            } else {
                throw new IllegalQueryOperationException("Unsupported tuple assignment in update query with joins.");
            }
        }
        for (TableGroup root : statement.getFromClause().getRoots()) {
            inlineView.getFromClause().addRoot(root);
        }
        inlineView.applyPredicate(statement.getRestriction());

        return new QueryPartTableReference(
                new SelectStatement(inlineView),
                "t",
                columnNames,
                false,
                getSessionFactory()
        );
    }

    protected void visitValuesList(List<Values> valuesList) {
        visitValuesListStandard(valuesList);
    }

    protected final void visitValuesListStandard(List<Values> valuesList) {
        if (valuesList.size() != 1 && !dialect.supportsValuesListForInsert()) {
            throw new IllegalQueryOperationException("Dialect does not support values lists for insert statements");
        }
        appendSql("values");
        final Stack<Clause> clauseStack = getClauseStack();
        try {
            clauseStack.push(Clause.VALUES);
            for (int i = 0; i < valuesList.size(); i++) {
                if (i != 0) {
                    appendSql(COMMA_SEPARATOR_CHAR);
                }
                appendSql(" (");
                final List<Expression> expressions = valuesList.get(i).getExpressions();
                for (int j = 0; j < expressions.size(); j++) {
                    if (j != 0) {
                        appendSql(COMMA_SEPARATOR_CHAR);
                    }
                    expressions.get(j).accept(this);
                }
                appendSql(')');
            }
        } finally {
            clauseStack.pop();
        }
    }

    protected void visitForUpdateClause(QuerySpec querySpec) {
        if (querySpec.isRoot()) {
            if (forUpdate != null) {
                final Boolean followOnLocking = getLockOptions() == null ?
                        Boolean.FALSE :
                        getLockOptions().getFollowOnLocking();
                if (Boolean.TRUE.equals(followOnLocking)) {
                    lockOptions = null;
                } else {
                    forUpdate.merge(getLockOptions());
                    forUpdate.applyAliases(dialect.getWriteRowLockStrategy(), querySpec);
                    if (LockMode.READ.lessThan(forUpdate.getLockMode())) {
                        final LockStrategy lockStrategy = determineLockingStrategy(
                                querySpec,
                                forUpdate,
                                followOnLocking
                        );
                        switch (lockStrategy) {
                            case CLAUSE:
                                renderForUpdateClause(querySpec, forUpdate);
                                break;
                            case FOLLOW_ON:
                                lockOptions = null;
                                break;
                        }
                    }
                }
                forUpdate = null;
            } else {
                // Since we get here, we know that no alias locks were applied.
                // We only apply locking on the root query though if there is a global lock mode
                final LockOptions lockOptions = getLockOptions();
                final Boolean followOnLocking = lockOptions == null ? Boolean.FALSE : lockOptions.getFollowOnLocking();
                if (Boolean.TRUE.equals(followOnLocking)) {
                    this.lockOptions = null;
                } else if (lockOptions != null && lockOptions.getLockMode() != LockMode.NONE) {
                    final ForUpdateClause forUpdateClause = new ForUpdateClause();
                    forUpdateClause.merge(getLockOptions());
                    forUpdateClause.applyAliases(dialect.getWriteRowLockStrategy(), querySpec);
                    if (LockMode.READ.lessThan(forUpdateClause.getLockMode())) {
                        final LockStrategy lockStrategy = determineLockingStrategy(
                                querySpec,
                                forUpdateClause,
                                followOnLocking
                        );
                        switch (lockStrategy) {
                            case CLAUSE:
                                renderForUpdateClause(
                                        querySpec,
                                        forUpdateClause
                                );
                                break;
                            case FOLLOW_ON:
                                if (Boolean.FALSE.equals(followOnLocking)) {
                                    throw new UnsupportedOperationException("");
                                }
                                this.lockOptions = null;
                                break;
                        }
                    }
                }
            }
        } else if (forUpdate != null) {
            forUpdate.merge(getLockOptions());
            forUpdate.applyAliases(dialect.getWriteRowLockStrategy(), querySpec);
            if (LockMode.READ.lessThan(forUpdate.getLockMode())) {
                final LockStrategy lockStrategy = determineLockingStrategy(querySpec, forUpdate, null);
                switch (lockStrategy) {
                    case CLAUSE:
                        renderForUpdateClause(querySpec, forUpdate);
                        break;
                    case FOLLOW_ON:
                        throw new UnsupportedOperationException("Follow-on locking for subqueries is not supported");
                }
            }
            forUpdate = null;
        }
    }

    protected void renderForUpdateClause(QuerySpec querySpec, ForUpdateClause forUpdateClause) {
        int timeoutMillis = forUpdateClause.getTimeoutMillis();
        LockKind lockKind = LockKind.NONE;
        switch (forUpdateClause.getLockMode()) {
            case PESSIMISTIC_WRITE:
                lockKind = LockKind.UPDATE;
                break;
            case PESSIMISTIC_READ:
                lockKind = LockKind.SHARE;
                break;
            case UPGRADE_NOWAIT:
            case PESSIMISTIC_FORCE_INCREMENT:
                timeoutMillis = LockOptions.NO_WAIT;
                lockKind = LockKind.UPDATE;
                break;
            case UPGRADE_SKIPLOCKED:
                timeoutMillis = LockOptions.SKIP_LOCKED;
                lockKind = LockKind.UPDATE;
                break;
            default:
                break;
        }
        if (lockKind != LockKind.NONE) {
            if (lockKind == LockKind.SHARE) {
                appendSql(getForShare(timeoutMillis));
                if (forUpdateClause.hasAliases() && dialect.getReadRowLockStrategy() != RowLockStrategy.NONE) {
                    appendSql(" of ");
                    forUpdateClause.appendAliases(this);
                }
            } else {
                appendSql(getForUpdate());
                if (forUpdateClause.hasAliases() && dialect.getWriteRowLockStrategy() != RowLockStrategy.NONE) {
                    appendSql(" of ");
                    forUpdateClause.appendAliases(this);
                }
            }
            appendSql(getForUpdateWithClause());
            switch (timeoutMillis) {
                case LockOptions.NO_WAIT:
                    if (dialect.supportsNoWait()) {
                        appendSql(getNoWait());
                    }
                    break;
                case LockOptions.SKIP_LOCKED:
                    if (dialect.supportsSkipLocked()) {
                        appendSql(getSkipLocked());
                    }
                    break;
                case LockOptions.WAIT_FOREVER:
                    break;
                default:
                    if (dialect.supportsWait()) {
                        appendSql(" wait ");
                        appendSql(Math.round(timeoutMillis / 1e3f));
                    }
                    break;
            }
        }
    }

    private enum LockKind {
        NONE,
        SHARE,
        UPDATE
    }

    protected String getForUpdate() {
        return " for update";
    }

    protected String getForShare(int timeoutMillis) {
        return " for update";
    }

    protected String getForUpdateWithClause() {
        // This is a clause to specify the lock isolation for e.g. Derby
        return "";
    }

    protected String getNoWait() {
        return " nowait";
    }

    protected String getSkipLocked() {
        return " skip locked";
    }

    protected LockMode getEffectiveLockMode(String alias) {
        final QueryPart currentQueryPart = getQueryPartStack().getCurrent();
        return currentQueryPart == null
                ? LockMode.NONE
                : getEffectiveLockMode(alias, currentQueryPart.isRoot());
    }

    protected LockMode getEffectiveLockMode(String alias, boolean isRoot) {
        if (getLockOptions() == null) {
            return LockMode.NONE;
        }
        LockMode lockMode = getLockOptions().getAliasSpecificLockMode(alias);
        if (isRoot && lockMode == null) {
            lockMode = getLockOptions().getLockMode();
        }
        return lockMode == null ? LockMode.NONE : lockMode;
    }

    protected boolean hasAggregateFunctions(QuerySpec querySpec) {
        return AggregateFunctionChecker.hasAggregateFunctions(querySpec);
    }

    protected LockStrategy determineLockingStrategy(
            QuerySpec querySpec,
            ForUpdateClause forUpdateClause,
            Boolean followOnLocking) {
        LockStrategy strategy = LockStrategy.CLAUSE;
        if (!querySpec.getGroupByClauseExpressions().isEmpty()) {
            if (Boolean.FALSE.equals(followOnLocking)) {
                throw new IllegalQueryOperationException("Locking with GROUP BY is not supported");
            }
            strategy = LockStrategy.FOLLOW_ON;
        }
        if (querySpec.getHavingClauseRestrictions() != null) {
            if (Boolean.FALSE.equals(followOnLocking)) {
                throw new IllegalQueryOperationException("Locking with HAVING is not supported");
            }
            strategy = LockStrategy.FOLLOW_ON;
        }
        if (querySpec.getSelectClause().isDistinct()) {
            if (Boolean.FALSE.equals(followOnLocking)) {
                throw new IllegalQueryOperationException("Locking with DISTINCT is not supported");
            }
            strategy = LockStrategy.FOLLOW_ON;
        }
        if (!dialect.supportsOuterJoinForUpdate()) {
            if (forUpdateClause.hasAliases()) {
                // Only need to visit the TableGroupJoins for which the alias is registered
                if (querySpec.getFromClause().queryTableGroupJoins(
                        tableGroupJoin -> {
                            final TableGroup group = tableGroupJoin.getJoinedGroup();
                            if (forUpdateClause.hasAlias(group.getSourceAlias())) {
                                if (tableGroupJoin.isInitialized() && tableGroupJoin.getJoinType() != SqlAstJoinType.INNER && !group.isVirtual()) {
                                    if (Boolean.FALSE.equals(followOnLocking)) {
                                        throw new IllegalQueryOperationException(
                                                "Locking with OUTER joins is not supported");
                                    }
                                    return Boolean.TRUE;
                                }
                            }
                            return null;
                        }
                ) != null) {
                    strategy = LockStrategy.FOLLOW_ON;
                }
            } else {
                // Visit TableReferenceJoin and TableGroupJoin to see if all use INNER
                if (querySpec.getFromClause().queryTableJoins(
                        tableJoin -> {
                            if (tableJoin.isInitialized() && tableJoin.getJoinType() != SqlAstJoinType.INNER && !(tableJoin.getJoinedNode() instanceof VirtualTableGroup)) {
                                if (Boolean.FALSE.equals(followOnLocking)) {
                                    throw new IllegalQueryOperationException(
                                            "Locking with OUTER joins is not supported");
                                }
                                return Boolean.TRUE;
                            }
                            return null;
                        }
                ) != null) {
                    strategy = LockStrategy.FOLLOW_ON;
                }
            }
        }
        if (hasAggregateFunctions(querySpec)) {
            if (Boolean.FALSE.equals(followOnLocking)) {
                throw new IllegalQueryOperationException("Locking with aggregate functions is not supported");
            }
            strategy = LockStrategy.FOLLOW_ON;
        }
        return strategy;
    }

    private void renderPredicatedSetAssignments(List<Assignment> assignments, Predicate predicate) {
        char separator = ' ';
        try {
            clauseStack.push(Clause.SET);
            for (Assignment assignment : assignments) {
                appendSql(separator);
                separator = COMMA_SEPARATOR_CHAR;
                if (predicate == null) {
                    visitSetAssignment(assignment);
                } else {
                    assert assignment.getAssignable().getColumnReferences().size() == 1;
                    final Expression expression = new CaseSearchedExpression(
                            (MappingModelExpressible) assignment.getAssignedValue().getExpressionType(),
                            List.of(
                                    new CaseSearchedExpression.WhenFragment(
                                            predicate, assignment.getAssignedValue()
                                    )
                            ),
                            assignment.getAssignable().getColumnReferences().get(0)
                    );
                    visitSetAssignment(new Assignment(assignment.getAssignable(), expression));
                }
            }
        } finally {
            clauseStack.pop();
        }
    }

    protected void visitReturningColumns(Supplier<List<ColumnReference>> returningColumnsAccess) {
        final List<ColumnReference> returningColumns = returningColumnsAccess.get();

        if (returningColumns.isEmpty()) {
            return;
        }

        visitReturningColumns(returningColumns);
    }

    protected void visitReturningColumns(List<ColumnReference> returningColumns) {
        final int size = returningColumns.size();
        if (size == 0) {
            return;
        }

        appendSql(" returning ");
        String separator = "";
        for (int i = 0; i < size; i++) {
            appendSql(separator);
            appendSql(returningColumns.get(i).getColumnExpression());
            separator = COMMA_SEPARATOR;
        }
    }

    private Expression abs(Expression expression) {
        final AbstractSqmSelfRenderingFunctionDescriptor abs = findSelfRenderingFunction("abs", 2);
        return new SelfRenderingFunctionSqlAstExpression(
                "abs",
                abs,
                List.of(expression),
                (ReturnableType<?>) expression.getExpressionType(),
                expression.getExpressionType()
        );
    }

    private Expression lpad(Expression expression, int stringLength, String padString) {
        final BasicType<String> stringType = getStringType();
        final AbstractSqmSelfRenderingFunctionDescriptor lpad = findSelfRenderingFunction("lpad", 3);
        return new SelfRenderingFunctionSqlAstExpression(
                "lpad",
                lpad,
                List.of(
                        expression,
                        new QueryLiteral<>(stringLength, getIntegerType()),
                        new QueryLiteral<>(padString, stringType)
                ),
                stringType,
                stringType
        );
    }

    private AbstractSqmSelfRenderingFunctionDescriptor findSelfRenderingFunction(
            String functionName,
            int argumentCount) {
        final SqmFunctionDescriptor functionDescriptor = sessionFactory.getQueryEngine()
                .getSqmFunctionRegistry()
                .findFunctionDescriptor(functionName);
        if (functionDescriptor instanceof MultipatternSqmFunctionDescriptor multiPatternFunction) {
            return (AbstractSqmSelfRenderingFunctionDescriptor) multiPatternFunction.getFunction(argumentCount);
        }
        return (AbstractSqmSelfRenderingFunctionDescriptor) functionDescriptor;
    }

    private Expression castToString(SqlAstNode node) {
        final BasicType<String> stringType = getStringType();
        return new SelfRenderingFunctionSqlAstExpression(
                "cast",
                castFunction(),
                List.of(node, new CastTarget(stringType)),
                stringType,
                stringType
        );
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // QuerySpec

    @Override
    public void visitQueryGroup(QueryGroup queryGroup) {
        renderQueryGroup(queryGroup, true);
    }

    protected void renderQueryGroup(QueryGroup queryGroup, boolean renderOrderByAndOffsetFetchClause) {
        final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
        final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
        final boolean needsSelectAliases = this.needsSelectAliases;
        try {
            String queryGroupAlias = null;
            // See the field documentation of queryPartForRowNumbering etc. for an explanation about this
            final QueryPart currentQueryPart = queryPartStack.getCurrent();
            if (currentQueryPart != null && queryPartForRowNumberingClauseDepth != clauseStack.depth()) {
                this.queryPartForRowNumbering = null;
                this.queryPartForRowNumberingClauseDepth = -1;
                // If explicit column aliases were defined we should still use them when rendering the select clause
                this.needsSelectAliases = columnAliases != null;
            }
            // If we are row numbering the current query group, this means that we can't render the
            // order by and offset fetch clause, so we must do row counting on the query group level
            final boolean needsRowNumberingWrapper = queryPartForRowNumbering == queryGroup
                    || additionalWherePredicate != null && !additionalWherePredicate.isEmpty();
            final boolean needsQueryGroupWrapper = currentQueryPart instanceof QueryGroup && !supportsSimpleQueryGrouping();
            final boolean needsParenthesis;
            if (currentQueryPart instanceof QueryGroup) {
                // When this is query group within a query group, we can only do simple grouping if that is supported,
                // and we don't already add a query group wrapper
                needsParenthesis = !needsRowNumberingWrapper && !needsQueryGroupWrapper;
            } else {
                needsParenthesis = queryGroup.hasOffsetOrFetchClause() && !queryGroup.isRoot();
            }
            if (needsParenthesis) {
                appendSql(OPEN_PARENTHESIS);
            }
            if (needsRowNumberingWrapper) {
                this.needsSelectAliases = true;
                queryGroupAlias = "grp_" + queryGroupAliasCounter + '_';
                queryGroupAliasCounter++;
                appendSql("select ");
                appendSql(queryGroupAlias);
                appendSql(".* ");
                final SelectClause firstSelectClause = queryGroup.getFirstQuerySpec().getSelectClause();
                final List<SqlSelection> sqlSelections = firstSelectClause.getSqlSelections();
                final int sqlSelectionsSize = sqlSelections.size();
                // We need this synthetic select clause to properly render the ORDER BY within the OVER clause
                // of the row numbering functions
                final SelectClause syntheticSelectClause = new SelectClause(sqlSelectionsSize);
                for (int i = 0; i < sqlSelectionsSize; i++) {
                    syntheticSelectClause.addSqlSelection(
                            new SqlSelectionImpl(
                                    i,
                                    new ColumnReference(
                                            queryGroupAlias,
                                            "c" + i,
                                            false,
                                            null,
                                            getIntegerType()
                                    )
                            )
                    );
                }
                renderRowNumberingSelectItems(syntheticSelectClause, queryPartForRowNumbering);
                appendSql(" from (");
            } else if (needsQueryGroupWrapper) {
                // Query group nested inside a query group
                this.needsSelectAliases = true;
                queryGroupAlias = "grp_" + queryGroupAliasCounter + '_';
                queryGroupAliasCounter++;
                appendSql("select ");
                appendSql(queryGroupAlias);
                appendSql(".* ");
                appendSql(" from (");
            }
            queryPartStack.push(queryGroup);
            final List<QueryPart> queryParts = queryGroup.getQueryParts();
            final String setOperatorString = ' ' + queryGroup.getSetOperator().sqlString() + ' ';
            String separator = "";
            for (int i = 0; i < queryParts.size(); i++) {
                appendSql(separator);
                queryParts.get(i).accept(this);
                separator = setOperatorString;
            }

            if (renderOrderByAndOffsetFetchClause) {
                visitOrderBy(queryGroup.getSortSpecifications());
                visitOffsetFetchClause(queryGroup);
            }
            if (queryGroupAlias != null) {
                appendSql(") ");
                appendSql(queryGroupAlias);
                if (additionalWherePredicate != null && !additionalWherePredicate.isEmpty()) {
                    visitWhereClause(additionalWherePredicate);
                }
            }
            if (needsParenthesis) {
                appendSql(CLOSE_PARENTHESIS);
            }
        } finally {
            queryPartStack.pop();
            this.queryPartForRowNumbering = queryPartForRowNumbering;
            this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
            this.needsSelectAliases = needsSelectAliases;
        }
    }

    @Override
    public void visitQuerySpec(QuerySpec querySpec) {
        final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
        final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
        final boolean needsSelectAliases = this.needsSelectAliases;
        final Predicate additionalWherePredicate = this.additionalWherePredicate;
        final ForUpdateClause forUpdate = this.forUpdate;
        try {
            this.additionalWherePredicate = null;
            this.forUpdate = null;
            // See the field documentation of queryPartForRowNumbering etc. for an explanation about this
            // In addition, we also reset the row numbering if the currently row numbered query part is a query group
            // which means this query spec is a part of that query group.
            // We want the row numbering to happen on the query group level, not on the query spec level, so we reset
            final QueryPart currentQueryPart = queryPartStack.getCurrent();
            if (currentQueryPart != null && (queryPartForRowNumbering instanceof QueryGroup || queryPartForRowNumberingClauseDepth != clauseStack.depth())) {
                this.queryPartForRowNumbering = null;
                this.queryPartForRowNumberingClauseDepth = -1;
            }
            String queryGroupAlias = null;
            if (currentQueryPart instanceof QueryGroup) {
                // We always need query wrapping if we are in a query group and this query spec has a fetch or order by
                // clause, because of order by precedence in SQL
                if (querySpec.hasOffsetOrFetchClause() || querySpec.hasSortSpecifications()) {
                    queryGroupAlias = "";
                    // If the parent is a query group with a fetch clause we must use a select wrapper,
                    // or if the database does not support simple query grouping, we must use a select wrapper
                    if ((!supportsSimpleQueryGrouping() || currentQueryPart.hasOffsetOrFetchClause())
                            // We can skip it though if this query spec is being row numbered,
                            // because then we already have a wrapper
                            && queryPartForRowNumbering != querySpec) {
                        queryGroupAlias = " grp_" + queryGroupAliasCounter + '_';
                        queryGroupAliasCounter++;
                        appendSql("select");
                        appendSql(queryGroupAlias);
                        appendSql(".* from ");
                        // We need to assign aliases when we render a query spec as subquery to avoid clashing aliases
                        this.needsSelectAliases = this.needsSelectAliases || hasDuplicateSelectItems(querySpec);
                    } else if (!supportsDuplicateSelectItemsInQueryGroup()) {
                        this.needsSelectAliases = this.needsSelectAliases || hasDuplicateSelectItems(querySpec);
                    }
                }
            }
            queryPartStack.push(querySpec);
            if (queryGroupAlias != null) {
                throw new NotSupportedRuntimeException("query group not supported");
            }
            appendSql('{');
            {
                appendSql(" find: ");
                visitFromClause(querySpec.getFromClause());
                appendSql(", filter: ");
                visitWhereClause(querySpec.getWhereClauseRestrictions());
                if (CollectionUtil.isNotEmpty(querySpec.getSortSpecifications())) {
                    appendSql(", sort: ");
                    visitOrderBy(querySpec.getSortSpecifications());
                }
                append(", projection: ");
                visitSelectClause(querySpec.getSelectClause());
                //visitGroupByClause( querySpec, dialect.getGroupBySelectItemReferenceStrategy() );
                //visitHavingClause( querySpec );
                visitOffsetFetchClause(querySpec);
                // We render the FOR UPDATE clause in the parent query
                //if ( queryPartForRowNumbering == null ) {
                //visitForUpdateClause( querySpec );
                //}
            }
            appendSql(" }");
        } finally {
            this.queryPartStack.pop();
            this.queryPartForRowNumbering = queryPartForRowNumbering;
            this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
            this.needsSelectAliases = needsSelectAliases;
            this.additionalWherePredicate = additionalWherePredicate;
            if (queryPartForRowNumbering == null) {
                this.forUpdate = forUpdate;
            }
        }
    }

    private boolean hasDuplicateSelectItems(QuerySpec querySpec) {
        final List<SqlSelection> sqlSelections = querySpec.getSelectClause().getSqlSelections();
        final Map<Expression, Boolean> map = new IdentityHashMap<>(sqlSelections.size());
        for (int i = 0; i < sqlSelections.size(); i++) {
            if (map.put(sqlSelections.get(i).getExpression(), Boolean.TRUE) != null) {
                return true;
            }
        }
        return false;
    }

    protected boolean supportsSimpleQueryGrouping() {
        return true;
    }

    protected boolean supportsDuplicateSelectItemsInQueryGroup() {
        return true;
    }

    protected final void visitWhereClause(Predicate whereClauseRestrictions) {
        final Predicate additionalWherePredicate = this.additionalWherePredicate;
        boolean existsWhereClauseRestrictions = whereClauseRestrictions != null && !whereClauseRestrictions.isEmpty();
        boolean existsAdditionalWherePredicate = additionalWherePredicate != null;
        boolean requiredAndPredicate = existsWhereClauseRestrictions && existsAdditionalWherePredicate;
        if (existsWhereClauseRestrictions || existsAdditionalWherePredicate) {
            if (requiredAndPredicate) {
                appendSql("{ $and: [ ");
            }
            clauseStack.push(Clause.WHERE);
            try {
                if (existsWhereClauseRestrictions) {
                    whereClauseRestrictions.accept(this);
                }
                if (requiredAndPredicate) {
                    appendSql(", ");
                }
                if (existsAdditionalWherePredicate) {
                    this.additionalWherePredicate = null;
                    additionalWherePredicate.accept(this);
                }
                if (requiredAndPredicate) {
                    appendSql(" ] }");
                }
            } finally {
                clauseStack.pop();
            }
        }
    }

    protected Expression resolveAliasedExpression(Expression expression) {
        // This can happen when using window functions for emulating the offset/fetch clause of a query group
        // But in that case we always use a SqlSelectionExpression anyway, so this is fine as it doesn't need resolving
        if (queryPartStack.getCurrent() == null) {
            assert expression instanceof SqlSelectionExpression;
            return ((SqlSelectionExpression) expression).getSelection().getExpression();
        }
        return resolveAliasedExpression(
                queryPartStack.getCurrent().getFirstQuerySpec().getSelectClause().getSqlSelections(),
                expression
        );
    }

    protected Expression resolveAliasedExpression(List<SqlSelection> sqlSelections, Expression expression) {
        if (expression instanceof Literal) {
            Object literalValue = ((Literal) expression).getLiteralValue();
            if (literalValue instanceof Integer) {
                return sqlSelections.get((Integer) literalValue).getExpression();
            }
        } else if (expression instanceof SqlSelectionExpression) {
            return ((SqlSelectionExpression) expression).getSelection().getExpression();
        } else if (expression instanceof SqmPathInterpretation<?>) {
            final Expression sqlExpression = ((SqmPathInterpretation<?>) expression).getSqlExpression();
            if (sqlExpression instanceof SqlSelectionExpression) {
                return ((SqlSelectionExpression) sqlExpression).getSelection().getExpression();
            }
        }
        return expression;
    }

    protected Expression resolveExpressionToAlias(Expression expression) {
        int index = -1;
        if (expression instanceof SqlSelectionExpression) {
            index = ((SqlSelectionExpression) expression).getSelection().getValuesArrayPosition();
        } else if (expression instanceof SqmPathInterpretation<?>) {
            final Expression sqlExpression = ((SqmPathInterpretation<?>) expression).getSqlExpression();
            if (sqlExpression instanceof SqlSelectionExpression) {
                index = ((SqlSelectionExpression) sqlExpression).getSelection().getValuesArrayPosition();
            }
        }
        if (index == -1) {
            return expression;
        }
        return new ColumnReference(
                (String) null,
                "c" + index,
                false,
                null,
                expression.getExpressionType().getSingleJdbcMapping()
        );
    }

    protected final void visitGroupByClause(QuerySpec querySpec, SelectItemReferenceStrategy referenceStrategy) {
        final List<Expression> partitionExpressions = querySpec.getGroupByClauseExpressions();
        if (!partitionExpressions.isEmpty()) {
            try {
                clauseStack.push(Clause.GROUP);
                appendSql(" group by ");
                visitPartitionExpressions(partitionExpressions, referenceStrategy);
            } finally {
                clauseStack.pop();
            }
        }
    }

    protected final void visitPartitionByClause(List<Expression> partitionExpressions) {
        if (!partitionExpressions.isEmpty()) {
            try {
                clauseStack.push(Clause.PARTITION);
                appendSql("partition by ");
                visitPartitionExpressions(partitionExpressions, SelectItemReferenceStrategy.EXPRESSION);
            } finally {
                clauseStack.pop();
            }
        }
    }

    protected final void visitPartitionExpressions(
            List<Expression> partitionExpressions,
            SelectItemReferenceStrategy referenceStrategy) {
        final Function<Expression, Expression> resolveAliasExpression;
        final boolean inlineParametersOfAliasedExpressions;
        switch (referenceStrategy) {
            case POSITION:
                resolveAliasExpression = Function.identity();
                inlineParametersOfAliasedExpressions = false;
                break;
            case ALIAS:
                resolveAliasExpression = this::resolveExpressionToAlias;
                inlineParametersOfAliasedExpressions = false;
                break;
            case EXPRESSION:
            default:
                resolveAliasExpression = this::resolveAliasedExpression;
                inlineParametersOfAliasedExpressions = true;
                break;
        }
        visitPartitionExpressions(partitionExpressions, resolveAliasExpression, inlineParametersOfAliasedExpressions);
    }

    protected final void visitPartitionExpressions(
            List<Expression> partitionExpressions,
            Function<Expression, Expression> resolveAliasExpression,
            boolean inlineParametersOfAliasedExpressions) {
        String separator = "";
        for (Expression partitionExpression : partitionExpressions) {
            final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(partitionExpression);
            if (sqlTuple != null) {
                for (Expression e : sqlTuple.getExpressions()) {
                    appendSql(separator);
                    final Expression resolved = resolveAliasExpression.apply(e);
                    if (inlineParametersOfAliasedExpressions && resolved != e) {
                        final SqlAstNodeRenderingMode original = parameterRenderingMode;
                        parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
                        renderPartitionItem(resolved);
                        parameterRenderingMode = original;
                    } else {
                        renderPartitionItem(resolved);
                    }
                    separator = COMMA_SEPARATOR;
                }
            } else {
                appendSql(separator);
                final Expression resolved = resolveAliasExpression.apply(partitionExpression);
                if (inlineParametersOfAliasedExpressions && resolved != partitionExpression) {
                    final SqlAstNodeRenderingMode original = parameterRenderingMode;
                    parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
                    renderPartitionItem(resolved);
                    parameterRenderingMode = original;
                } else {
                    renderPartitionItem(resolved);
                }
            }
            separator = COMMA_SEPARATOR;
        }
    }

    protected void renderPartitionItem(Expression expression) {
        if (expression instanceof Literal) {
            appendSql("()");
        } else if (expression instanceof Summarization summarization) {
            appendSql(summarization.getKind().sqlText());
            appendSql(OPEN_PARENTHESIS);
            renderCommaSeparated(summarization.getGroupings());
            appendSql(CLOSE_PARENTHESIS);
        } else {
            expression.accept(this);
        }
    }

    protected final void visitHavingClause(QuerySpec querySpec) {
        final Predicate havingClauseRestrictions = querySpec.getHavingClauseRestrictions();
        if (havingClauseRestrictions != null && !havingClauseRestrictions.isEmpty()) {
            appendSql(" having ");

            clauseStack.push(Clause.HAVING);
            try {
                havingClauseRestrictions.accept(this);
            } finally {
                clauseStack.pop();
            }
        }
    }

    protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
        // If we have a query part for row numbering, there is no need to render the order by clause
        // as that is part of the row numbering window function already, by which we then order by in the outer query
        appendSql('{');
        if (queryPartForRowNumbering == null) {
            renderOrderBy(true, sortSpecifications);
        }
        appendSql(" }");
    }

    protected void renderOrderBy(boolean addWhitespace, List<SortSpecification> sortSpecifications) {
        if (sortSpecifications != null && !sortSpecifications.isEmpty()) {
            if (addWhitespace) {
                appendSql(WHITESPACE);
            }
            clauseStack.push(Clause.ORDER);
            try {
                String separator = NO_SEPARATOR;
                for (SortSpecification sortSpecification : sortSpecifications) {
                    appendSql(separator);
                    visitSortSpecification(sortSpecification);
                    separator = COMMA_SEPARATOR;
                }
            } finally {
                clauseStack.pop();
            }
        }
    }

    /**
     * A tuple comparison like <code>(a, b) &gt; (1, 2)</code> can be emulated through it logical definition: <code>a &gt; 1 or a = 1 and b &gt; 2</code>.
     * The normal tuple comparison emulation is not very index friendly though because of the top level OR predicate.
     * Index optimized emulation of tuple comparisons puts an AND predicate on the top level.
     * The effect of that is, that the database can do an index seek to efficiently find a superset of matching rows.
     * Generally, it is sufficient to just add a broader predicate like for <code>(a, b) &gt; (1, 2)</code> we add <code>a &gt;= 1 and (..)</code>.
     * But we can further optimize this if we just remove the non-matching parts from this too broad predicate.
     * For <code>(a, b, c) &gt; (1, 2, 3)</code> we use the broad predicate <code>a &gt;= 1</code> and then want to remove rows where <code>a = 1 and (b, c) &lt;= (2, 3)</code>
     */
    protected void emulateTupleComparison(
            final List<? extends SqlAstNode> lhsExpressions,
            final List<? extends SqlAstNode> rhsExpressions,
            ComparisonOperator operator,
            boolean indexOptimized) {
        final boolean isCurrentWhereClause = clauseStack.getCurrent() == Clause.WHERE;
        if (isCurrentWhereClause) {
            appendSql(OPEN_PARENTHESIS);
        }

        final int size = lhsExpressions.size();
        assert size == rhsExpressions.size();
        switch (operator) {
            case DISTINCT_FROM:
                appendSql("not ");
            case NOT_DISTINCT_FROM: {
                if (supportsIntersect()) {
                    appendSql("exists (select ");
                    renderCommaSeparatedSelectExpression(lhsExpressions);
                    appendSql(getFromDualForSelectOnly());
                    appendSql(" intersect select ");
                    renderCommaSeparatedSelectExpression(rhsExpressions);
                    appendSql(getFromDualForSelectOnly());
                    appendSql(CLOSE_PARENTHESIS);
                } else {
                    appendSql("exists (select 1 from ");
                    appendSql(getDual());
                    appendSql(" d_ where (");
                    String separator = NO_SEPARATOR;
                    for (int i = 0; i < size; i++) {
                        appendSql(separator);
                        lhsExpressions.get(i).accept(this);
                        appendSql('=');
                        rhsExpressions.get(i).accept(this);
                        appendSql(" or ");
                        lhsExpressions.get(i).accept(this);
                        appendSql(" is null and ");
                        rhsExpressions.get(i).accept(this);
                        appendSql(" is null");
                        separator = ") and (";
                    }
                    appendSql("))");
                }
                break;
            }
            case EQUAL: {
                final String operatorText = operator.sqlText();
                String separator = NO_SEPARATOR;
                for (int i = 0; i < size; i++) {
                    appendSql(separator);
                    lhsExpressions.get(i).accept(this);
                    appendSql(operatorText);
                    rhsExpressions.get(i).accept(this);
                    separator = " and ";
                }
                break;
            }
            case NOT_EQUAL: {
                final String operatorText = operator.sqlText();
                String separator = NO_SEPARATOR;
                for (int i = 0; i < size; i++) {
                    appendSql(separator);
                    lhsExpressions.get(i).accept(this);
                    appendSql(operatorText);
                    rhsExpressions.get(i).accept(this);
                    separator = " or ";
                }
                break;
            }
            case LESS_THAN_OR_EQUAL:
                // Optimized (a, b) <= (1, 2) as: a <= 1 and not (a = 1 and b > 2)
                // Normal    (a, b) <= (1, 2) as: a <  1 or a = 1 and (b <= 2)
            case GREATER_THAN_OR_EQUAL:
                // Optimized (a, b) >= (1, 2) as: a >= 1 and not (a = 1 and b < 2)
                // Normal    (a, b) >= (1, 2) as: a >  1 or a = 1 and (b >= 2)
            case LESS_THAN:
                // Optimized (a, b) <  (1, 2) as: a <= 1 and not (a = 1 and b >= 2)
                // Normal    (a, b) <  (1, 2) as: a <  1 or a = 1 and (b < 2)
            case GREATER_THAN: {
                // Optimized (a, b) >  (1, 2) as: a >= 1 and not (a = 1 and b <= 2)
                // Normal    (a, b) >  (1, 2) as: a >  1 or a = 1 and (b > 2)
                if (indexOptimized) {
                    lhsExpressions.get(0).accept(this);
                    appendSql(operator.broader().sqlText());
                    rhsExpressions.get(0).accept(this);
                    appendSql(" and not ");
                    final String negatedOperatorText = operator.negated().sqlText();
                    emulateTupleComparisonSimple(
                            lhsExpressions,
                            rhsExpressions,
                            negatedOperatorText,
                            negatedOperatorText,
                            true
                    );
                } else {
                    emulateTupleComparisonSimple(
                            lhsExpressions,
                            rhsExpressions,
                            operator.sharper().sqlText(),
                            operator.sqlText(),
                            false
                    );
                }
                break;
            }
        }

        if (isCurrentWhereClause) {
            appendSql(CLOSE_PARENTHESIS);
        }
    }

    protected boolean supportsIntersect() {
        return true;
    }

    protected boolean supportsNestedSubqueryCorrelation() {
        return true;
    }

    protected void renderExpressionsAsSubquery(final List<? extends Expression> expressions) {
        clauseStack.push(Clause.SELECT);

        try {
            appendSql("select ");

            renderCommaSeparatedSelectExpression(expressions);
            appendSql(getFromDualForSelectOnly());
        } finally {
            clauseStack.pop();
        }
    }

    private void emulateTupleComparisonSimple(
            final List<? extends SqlAstNode> lhsExpressions,
            final List<? extends SqlAstNode> rhsExpressions,
            final String operatorText,
            final String finalOperatorText,
            final boolean optimized) {
        // Render (a, b) OP (1, 2) as: (a OP 1 or a = 1 and b FINAL_OP 2)

        final int size = lhsExpressions.size();
        final int lastIndex = size - 1;

        appendSql(OPEN_PARENTHESIS);
        String separator = NO_SEPARATOR;

        int i;
        if (optimized) {
            i = 1;
        } else {
            lhsExpressions.get(0).accept(this);
            appendSql(operatorText);
            rhsExpressions.get(0).accept(this);
            separator = " or ";
            i = 1;
        }

        for (; i < lastIndex; i++) {
            // Render the equals parts
            appendSql(separator);
            lhsExpressions.get(i - 1).accept(this);
            appendSql('=');
            rhsExpressions.get(i - 1).accept(this);

            // Render the actual operator part for the current component
            appendSql(" and (");
            lhsExpressions.get(i).accept(this);
            appendSql(operatorText);
            rhsExpressions.get(i).accept(this);
            separator = " or ";
        }

        // Render the equals parts
        appendSql(separator);
        lhsExpressions.get(lastIndex - 1).accept(this);
        appendSql('=');
        rhsExpressions.get(lastIndex - 1).accept(this);

        // Render the actual operator part for the current component
        appendSql(" and ");
        lhsExpressions.get(lastIndex).accept(this);
        appendSql(finalOperatorText);
        rhsExpressions.get(lastIndex).accept(this);

        // Close all opened parenthesis
        for (i = optimized ? 1 : 0; i < lastIndex + 1; i++) {
            appendSql(CLOSE_PARENTHESIS);
        }
    }

    protected void renderSelectTupleComparison(
            final List<SqlSelection> lhsExpressions,
            SqlTuple tuple,
            ComparisonOperator operator) {
        renderTupleComparisonStandard(lhsExpressions, tuple, operator);
    }

    protected void renderTupleComparisonStandard(
            final List<SqlSelection> lhsExpressions,
            SqlTuple tuple,
            ComparisonOperator operator) {
        appendSql(OPEN_PARENTHESIS);
        String separator = NO_SEPARATOR;
        for (SqlSelection lhsExpression : lhsExpressions) {
            appendSql(separator);
            lhsExpression.getExpression().accept(this);
            separator = COMMA_SEPARATOR;
        }
        appendSql(CLOSE_PARENTHESIS);
        appendSql(operator.sqlText());
        tuple.accept(this);
    }

    protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
        renderComparisonStandard(lhs, operator, rhs);
    }

    protected void renderComparisonStandard(Expression lhs, ComparisonOperator operator, Expression rhs) {
        appendSql("{ ");
        lhs.accept(this);
        appendSql(": { ");
        appendSql(getMongodbOperatorText(operator));
        appendSql(": ");
        rhs.accept(this);
        appendSql(" } }");
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ORDER BY clause

    @Override
    public void visitSortSpecification(SortSpecification sortSpecification) {
        final Expression sortExpression = sortSpecification.getSortExpression();
        final NullPrecedence nullPrecedence = sortSpecification.getNullPrecedence();
        final SortDirection sortOrder = sortSpecification.getSortOrder();
        final boolean ignoreCase = sortSpecification.isIgnoreCase();
        final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(sortExpression);
        if (sqlTuple != null) {
            String separator = NO_SEPARATOR;
            for (Expression expression : sqlTuple.getExpressions()) {
                appendSql(separator);
                visitSortSpecification(expression, sortOrder, nullPrecedence, ignoreCase);
                separator = COMMA_SEPARATOR;
            }
        } else {
            visitSortSpecification(sortExpression, sortOrder, nullPrecedence, ignoreCase);
        }
    }

    protected void visitSortSpecification(
            Expression sortExpression,
            SortDirection sortOrder,
            NullPrecedence nullPrecedence,
            boolean ignoreCase) {
        if (nullPrecedence == null || nullPrecedence == NullPrecedence.NONE) {
            nullPrecedence = sessionFactory.getSessionFactoryOptions().getDefaultNullPrecedence();
        }
        final boolean renderNullPrecedence = nullPrecedence != null &&
                !nullPrecedence.isDefaultOrdering(sortOrder, dialect.getNullOrdering());
        final boolean supportsNullPrecedence = renderNullPrecedence && supportsNullPrecedence();
        if (renderNullPrecedence && !supportsNullPrecedence) {
            emulateSortSpecificationNullPrecedence(sortExpression, nullPrecedence);
        }

        if (ignoreCase) {
            appendSql(dialect.getLowercaseFunction());
            appendSql(OPEN_PARENTHESIS);
        }

        if (inOverOrWithinGroupClause()) {
            resolveAliasedExpression(sortExpression).accept(this);
        } else {
            sortExpression.accept(this);
        }

        if (ignoreCase) {
            appendSql(CLOSE_PARENTHESIS);
        }

        appendSql(": ");
        if (sortOrder == SortDirection.DESCENDING) {
            appendSql(" -1");
        } else if (sortOrder == SortDirection.ASCENDING && renderNullPrecedence && supportsNullPrecedence) {
            appendSql(" 1");
        }
    }

    protected boolean supportsNullPrecedence() {
        return dialect.supportsNullPrecedence();
    }

    protected void emulateSortSpecificationNullPrecedence(Expression sortExpression, NullPrecedence nullPrecedence) {
        // TODO: generate "virtual" select items and use them here positionally
        appendSql("case when (");
        resolveAliasedExpression(sortExpression).accept(this);
        appendSql(") is null then ");
        if (nullPrecedence == NullPrecedence.FIRST) {
            appendSql("0 else 1");
        } else {
            appendSql("1 else 0");
        }
        appendSql(" end");
        appendSql(COMMA_SEPARATOR_CHAR);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // LIMIT/OFFSET/FETCH clause

    @Override
    public void visitOffsetFetchClause(QueryPart queryPart) {
        if (!isRowNumberingCurrentQueryPart()) {
            renderOffsetFetchClause(queryPart, true);
        }
    }

    protected void renderOffsetFetchClause(QueryPart queryPart, boolean renderOffsetRowsKeyword) {
        if (queryPart.isRoot() && hasLimit()) {
            prepareLimitOffsetParameters();
            renderOffsetFetchClause(
                    getOffsetParameter(),
                    getLimitParameter(),
                    FetchClauseType.ROWS_ONLY,
                    renderOffsetRowsKeyword
            );
        } else {
            renderOffsetFetchClause(
                    queryPart.getOffsetClauseExpression(),
                    queryPart.getFetchClauseExpression(),
                    queryPart.getFetchClauseType(),
                    renderOffsetRowsKeyword
            );
        }
    }

    protected void renderOffsetFetchClause(
            Expression offsetExpression,
            Expression fetchExpression,
            FetchClauseType fetchClauseType,
            boolean renderOffsetRowsKeyword) {
        if (offsetExpression != null) {
            renderOffset(offsetExpression, renderOffsetRowsKeyword);
        }

        if (fetchExpression != null) {
            renderFetch(fetchExpression, null, fetchClauseType);
        }
    }

    protected void renderOffset(Expression offsetExpression, boolean renderOffsetRowsKeyword) {
        appendSql(", skip: ");
        clauseStack.push(Clause.OFFSET);
        try {
            renderOffsetExpression(offsetExpression);
        } finally {
            clauseStack.pop();
        }
    }

    protected void renderFetch(
            Expression fetchExpression,
            Expression offsetExpressionToAdd,
            FetchClauseType fetchClauseType) {
        appendSql(", limit: ");
        clauseStack.push(Clause.FETCH);
        try {
            if (offsetExpressionToAdd == null) {
                renderFetchExpression(fetchExpression);
            } else {
                renderFetchPlusOffsetExpression(fetchExpression, offsetExpressionToAdd, 0);
            }
        } finally {
            clauseStack.pop();
        }
    }

    protected void renderOffsetExpression(Expression offsetExpression) {
        offsetExpression.accept(this);
    }

    protected void renderFetchExpression(Expression fetchExpression) {
        fetchExpression.accept(this);
    }

    protected void renderTopStartAtClause(
            Expression offsetExpression,
            Expression fetchExpression,
            FetchClauseType fetchClauseType) {
        if (fetchExpression != null) {
            appendSql("top ");
            final Stack<Clause> clauseStack = getClauseStack();
            clauseStack.push(Clause.FETCH);
            try {
                renderFetchExpression(fetchExpression);
            } finally {
                clauseStack.pop();
            }
            if (offsetExpression != null) {
                clauseStack.push(Clause.OFFSET);
                try {
                    appendSql(" start at ");
                    renderOffsetExpression(offsetExpression);
                } finally {
                    clauseStack.pop();
                }
            }
            appendSql(WHITESPACE);
            switch (fetchClauseType) {
                case ROWS_WITH_TIES:
                    appendSql("with ties ");
                    break;
                case PERCENT_ONLY:
                    appendSql("percent ");
                    break;
                case PERCENT_WITH_TIES:
                    appendSql("percent with ties ");
                    break;
            }
        }
    }

    protected void renderRowsToClause(Expression offsetClauseExpression, Expression fetchClauseExpression) {
        if (fetchClauseExpression != null) {
            appendSql("rows ");
            final Stack<Clause> clauseStack = getClauseStack();
            clauseStack.push(Clause.FETCH);
            try {
                renderFetchExpression(fetchClauseExpression);
            } finally {
                clauseStack.pop();
            }
            if (offsetClauseExpression != null) {
                clauseStack.push(Clause.OFFSET);
                try {
                    appendSql(" to ");
                    // According to RowsLimitHandler this is 1 based so we need to add 1 to the offset
                    renderFetchPlusOffsetExpression(fetchClauseExpression, offsetClauseExpression, 1);
                } finally {
                    clauseStack.pop();
                }
            }
            appendSql(WHITESPACE);
        }
    }

    protected void renderFetchPlusOffsetExpression(
            Expression fetchClauseExpression,
            Expression offsetClauseExpression,
            int offset) {
        renderFetchExpression(fetchClauseExpression);
        appendSql('+');
        renderOffsetExpression(offsetClauseExpression);
        if (offset != 0) {
            appendSql('+');
            appendSql(offset);
        }
    }

    protected boolean isRowNumberingCurrentQueryPart() {
        return queryPartForRowNumbering != null;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SELECT clause

    @Override
    public void visitSelectClause(SelectClause selectClause) {
        clauseStack.push(Clause.SELECT);
        appendSql("{ ");
        try {
			/*if ( selectClause.isDistinct() ) {
				appendSql( "distinct " );
			}*/
            visitSqlSelections(selectClause);
            appendSql(" }");
        } finally {
            clauseStack.pop();
        }
    }

    protected void visitSqlSelections(SelectClause selectClause) {
        final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
        final int size = sqlSelections.size();
        final SelectItemReferenceStrategy referenceStrategy = dialect.getGroupBySelectItemReferenceStrategy();
        // When the dialect needs to render the aliased expression and there are aliased group by items,
        // we need to inline parameters as the database would otherwise not be able to match the group by item
        // to the select item, ultimately leading to a query error
        final BitSet selectItemsToInline;
        if (referenceStrategy == SelectItemReferenceStrategy.EXPRESSION) {
            selectItemsToInline = getSelectItemsToInline();
        } else {
            selectItemsToInline = null;
        }
        final SqlAstNodeRenderingMode original = parameterRenderingMode;
        final SqlAstNodeRenderingMode defaultRenderingMode;
        if (getStatement() instanceof InsertSelectStatement && clauseStack.depth() == 1 && queryPartStack.depth() == 1) {
            // Databases support inferring parameter types for simple insert-select statements
            defaultRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
        } else {
            defaultRenderingMode = SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER;
        }
        if (needsSelectAliases || referenceStrategy == SelectItemReferenceStrategy.ALIAS && hasSelectAliasInGroupByClause()) {
            String separator = NO_SEPARATOR;
            int offset = 0;
            for (int i = 0; i < size; i++) {
                final SqlSelection sqlSelection = sqlSelections.get(i);
                if (sqlSelection.isVirtual()) {
                    continue;
                }
                if (selectItemsToInline != null && selectItemsToInline.get(i)) {
                    parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
                } else {
                    parameterRenderingMode = defaultRenderingMode;
                }
                final Expression expression = sqlSelection.getExpression();
                final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(expression);
                if (sqlTuple != null) {
                    final List<? extends Expression> expressions = sqlTuple.getExpressions();
                    for (Expression e : expressions) {
                        appendSql(separator);
                        renderSelectExpression(e);
                        appendSql(WHITESPACE);
                        if (columnAliases == null) {
                            appendSql('c');
                            appendSql(offset);
                        } else {
                            appendSql(columnAliases.get(offset));
                        }
                        offset++;
                        separator = COMMA_SEPARATOR;
                    }
                } else {
                    appendSql(separator);
                    renderSelectExpression(expression);
                    appendSql(WHITESPACE);
                    if (columnAliases == null) {
                        appendSql('c');
                        appendSql(offset);
                    } else {
                        appendSql(columnAliases.get(offset));
                    }
                    offset++;
                    separator = COMMA_SEPARATOR;
                }
                parameterRenderingMode = original;
            }
            if (queryPartForRowNumbering != null) {
                renderRowNumberingSelectItems(selectClause, queryPartForRowNumbering);
            }
        } else {
            assert columnAliases == null;
            String separator = NO_SEPARATOR;
            for (int i = 0; i < size; i++) {
                final SqlSelection sqlSelection = sqlSelections.get(i);
                if (sqlSelection.isVirtual()) {
                    continue;
                }
                appendSql(separator);
                if (selectItemsToInline != null && selectItemsToInline.get(i)) {
                    parameterRenderingMode = SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
                } else {
                    parameterRenderingMode = defaultRenderingMode;
                }
                visitSqlSelection(sqlSelection);
                appendSql(": 1");
                parameterRenderingMode = original;
                separator = ", ";
            }
        }
    }

    private BitSet getSelectItemsToInline() {
        final QuerySpec querySpec = (QuerySpec) getQueryPartStack().getCurrent();
        final List<SqlSelection> sqlSelections = querySpec.getSelectClause().getSqlSelections();
        final BitSet bitSet = new BitSet(sqlSelections.size());
        for (Expression groupByClauseExpression : querySpec.getGroupByClauseExpressions()) {
            final SqlSelectionExpression selectItemReference = getSelectItemReference(groupByClauseExpression);
            if (selectItemReference != null) {
                bitSet.set(sqlSelections.indexOf(selectItemReference.getSelection()));
            }
        }
        return bitSet;
    }

    private boolean hasSelectAliasInGroupByClause() {
        final QuerySpec querySpec = (QuerySpec) getQueryPartStack().getCurrent();
        for (Expression groupByClauseExpression : querySpec.getGroupByClauseExpressions()) {
            if (getSelectItemReference(groupByClauseExpression) != null) {
                return true;
            }
        }
        return false;
    }

    protected final SqlSelectionExpression getSelectItemReference(Expression expression) {
        final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(expression);
        if (sqlTuple != null) {
            for (Expression e : sqlTuple.getExpressions()) {
                if (e instanceof SqlSelectionExpression) {
                    return (SqlSelectionExpression) e;
                } else if (e instanceof SqmPathInterpretation<?>) {
                    final Expression sqlExpression = ((SqmPathInterpretation<?>) e).getSqlExpression();
                    if (sqlExpression instanceof SqlSelectionExpression) {
                        return (SqlSelectionExpression) sqlExpression;
                    }
                }
            }
        } else if (expression instanceof SqlSelectionExpression) {
            return (SqlSelectionExpression) expression;
        } else if (expression instanceof SqmPathInterpretation<?>) {
            final Expression sqlExpression = ((SqmPathInterpretation<?>) expression).getSqlExpression();
            if (sqlExpression instanceof SqlSelectionExpression) {
                return (SqlSelectionExpression) sqlExpression;
            }
        }
        return null;
    }

    protected void renderRowNumberingSelectItems(SelectClause selectClause, QueryPart queryPart) {
        final FetchClauseType fetchClauseType = getFetchClauseTypeForRowNumbering(queryPart);
        if (fetchClauseType != null) {
            appendSql(COMMA_SEPARATOR_CHAR);
            switch (fetchClauseType) {
                case PERCENT_ONLY:
                    appendSql("count(*) over () cnt,");
                case ROWS_ONLY:
                    renderRowNumber(selectClause, queryPart);
                    appendSql(" rn");
                    break;
                case PERCENT_WITH_TIES:
                    appendSql("count(*) over () cnt,");
                case ROWS_WITH_TIES:
                    if (queryPart.getOffsetClauseExpression() != null) {
                        renderRowNumber(selectClause, queryPart);
                        appendSql(" rn,");
                    }
                    if (selectClause.isDistinct()) {
                        appendSql("dense_rank()");
                    } else {
                        appendSql("rank()");
                    }
                    visitOverClause(
                            Collections.emptyList(),
                            getSortSpecificationsRowNumbering(selectClause, queryPart)
                    );
                    appendSql(" rnk");
                    break;
            }
        }
    }

    protected FetchClauseType getFetchClauseTypeForRowNumbering(QueryPart queryPartForRowNumbering) {
        if (queryPartForRowNumbering.isRoot() && hasLimit()) {
            return FetchClauseType.ROWS_ONLY;
        } else {
            return queryPartForRowNumbering.getFetchClauseType();
        }
    }

    @Override
    public void visitOver(Over<?> over) {
        final Expression overExpression = over.getExpression();
        overExpression.accept(this);
        final boolean orderedSetAggregate;
        if (overExpression instanceof OrderedSetAggregateFunctionExpression expression) {
            orderedSetAggregate = expression.getWithinGroup() != null && !expression.getWithinGroup().isEmpty();
        } else {
            orderedSetAggregate = false;
        }
        visitOverClause(
                over.getPartitions(),
                over.getOrderList(),
                over.getMode(),
                over.getStartKind(),
                over.getStartExpression(),
                over.getEndKind(),
                over.getEndExpression(),
                over.getExclusion(),
                orderedSetAggregate
        );
    }

    protected final void visitOverClause(
            List<Expression> partitionExpressions,
            List<SortSpecification> sortSpecifications) {
        visitOverClause(
                partitionExpressions,
                sortSpecifications,
                FrameMode.RANGE,
                FrameKind.UNBOUNDED_PRECEDING,
                null,
                FrameKind.CURRENT_ROW,
                null,
                FrameExclusion.NO_OTHERS,
                false
        );
    }

    protected void visitOverClause(
            List<Expression> partitionExpressions,
            List<SortSpecification> sortSpecifications,
            FrameMode mode,
            FrameKind startKind,
            Expression startExpression,
            FrameKind endKind,
            Expression endExpression,
            FrameExclusion exclusion,
            boolean orderedSetAggregate) {
        try {
            clauseStack.push(Clause.OVER);
            appendSql(" over(");
            visitPartitionByClause(partitionExpressions);
            if (!orderedSetAggregate) {
                renderOrderBy(!partitionExpressions.isEmpty(), sortSpecifications);
            }
            if (mode == FrameMode.RANGE && startKind == FrameKind.UNBOUNDED_PRECEDING && endKind == FrameKind.CURRENT_ROW && exclusion == FrameExclusion.NO_OTHERS) {
                // This is the default, so we don't need to render anything
            } else {
                if (!partitionExpressions.isEmpty() || !sortSpecifications.isEmpty()) {
                    append(WHITESPACE);
                }
                switch (mode) {
                    case GROUPS:
                        append("groups ");
                        break;
                    case RANGE:
                        append("range ");
                        break;
                    case ROWS:
                        append("rows ");
                        break;
                }
                if (endKind == FrameKind.CURRENT_ROW) {
                    renderFrameKind(startKind, startExpression);
                } else {
                    append("between ");
                    renderFrameKind(startKind, startExpression);
                    append(" and ");
                    renderFrameKind(endKind, endExpression);
                }
                switch (exclusion) {
                    case TIES:
                        append(" exclude ties");
                        break;
                    case CURRENT_ROW:
                        append(" exclude current row");
                        break;
                    case GROUP:
                        append(" exclude group");
                        break;
                }
            }
            appendSql(CLOSE_PARENTHESIS);
        } finally {
            clauseStack.pop();
        }
    }

    private void renderFrameKind(FrameKind kind, Expression expression) {
        switch (kind) {
            case CURRENT_ROW:
                append("current row");
                break;
            case UNBOUNDED_PRECEDING:
                append("unbounded preceding");
                break;
            case UNBOUNDED_FOLLOWING:
                append("unbounded following");
                break;
            case OFFSET_PRECEDING:
                expression.accept(this);
                append(" preceding");
                break;
            case OFFSET_FOLLOWING:
                expression.accept(this);
                append(" following");
                break;
            default:
                throw new UnsupportedOperationException("Unsupported frame kind: " + kind);
        }
    }

    protected void renderRowNumber(SelectClause selectClause, QueryPart queryPart) {
        if (selectClause.isDistinct()) {
            appendSql("dense_rank()");
        } else {
            appendSql("row_number()");
        }
        visitOverClause(Collections.emptyList(), getSortSpecificationsRowNumbering(selectClause, queryPart));
    }

    protected final boolean isParameter(Expression expression) {
        return expression instanceof JdbcParameter || expression instanceof SqmParameterInterpretation;
    }

    protected List<SortSpecification> getSortSpecificationsRowNumbering(
            SelectClause selectClause,
            QueryPart queryPart) {
        final List<SortSpecification> sortSpecifications;
        if (queryPart.hasSortSpecifications()) {
            sortSpecifications = queryPart.getSortSpecifications();
        } else {
            sortSpecifications = Collections.emptyList();
        }
        if (selectClause.isDistinct()) {
            // When select distinct is used, we need to add all select items to the order by clause
            final List<SqlSelection> sqlSelections = new ArrayList<>(selectClause.getSqlSelections());
            final int specificationsSize = sortSpecifications.size();
            for (int i = sqlSelections.size() - 1; i != 0; i--) {
                final Expression selectionExpression = sqlSelections.get(i).getExpression();
                for (int j = 0; j < specificationsSize; j++) {
                    final Expression expression = resolveAliasedExpression(
                            sqlSelections,
                            sortSpecifications.get(j).getSortExpression()
                    );
                    if (expression.equals(selectionExpression)) {
                        sqlSelections.remove(i);
                        break;
                    }
                }
            }
            final int sqlSelectionsSize = sqlSelections.size();
            if (sqlSelectionsSize == 0) {
                return sortSpecifications;
            } else {
                final List<SortSpecification> sortSpecificationsRowNumbering = new ArrayList<>(sqlSelectionsSize + specificationsSize);
                sortSpecificationsRowNumbering.addAll(sortSpecifications);
                for (int i = 0; i < sqlSelectionsSize; i++) {
                    sortSpecificationsRowNumbering.add(
                            new SortSpecification(
                                    new SqlSelectionExpression(sqlSelections.get(i)),
                                    SortDirection.ASCENDING,
                                    NullPrecedence.NONE
                            )
                    );
                }
                return sortSpecificationsRowNumbering;
            }
        } else if (queryPart instanceof QueryGroup) {
            // When the sort specifications come from a query group which uses positional references
            // we have to resolve to the actual selection expressions
            final int specificationsSize = sortSpecifications.size();
            final List<SortSpecification> sortSpecificationsRowNumbering = new ArrayList<>(specificationsSize);
            final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
            for (int i = 0; i < specificationsSize; i++) {
                final SortSpecification sortSpecification = sortSpecifications.get(i);
                final int position;
                if (sortSpecification.getSortExpression() instanceof SqlSelectionExpression) {
                    position = ((SqlSelectionExpression) sortSpecification.getSortExpression())
                            .getSelection()
                            .getValuesArrayPosition();
                } else {
                    assert sortSpecification.getSortExpression() instanceof QueryLiteral;
                    final QueryLiteral<?> queryLiteral = (QueryLiteral<?>) sortSpecification.getSortExpression();
                    assert queryLiteral.getLiteralValue() instanceof Integer;
                    position = (Integer) queryLiteral.getLiteralValue();
                }
                sortSpecificationsRowNumbering.add(
                        new SortSpecification(
                                new SqlSelectionExpression(
                                        sqlSelections.get(position)
                                ),
                                sortSpecification.getSortOrder(),
                                sortSpecification.getNullPrecedence()
                        )
                );
            }
            return sortSpecificationsRowNumbering;
        } else {
            return sortSpecifications;
        }
    }

    @Override
    public void visitSqlSelection(SqlSelection sqlSelection) {
        visitSqlSelectExpression(sqlSelection.getExpression());
    }

    protected void visitSqlSelectExpression(Expression expression) {
        final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(expression);
        if (sqlTuple != null) {
            boolean isFirst = true;
            for (Expression e : sqlTuple.getExpressions()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    appendSql(',');
                }
                renderSelectExpression(e);
            }
        } else {
            renderSelectExpression(expression);
        }
    }

    protected void renderSelectExpression(Expression expression) {
        renderExpressionAsClauseItem(expression);
    }

    protected void renderExpressionAsClauseItem(Expression expression) {
        // Most databases do not support predicates as top level items
        if (expression instanceof Predicate) {
            appendSql("case when ");
            expression.accept(this);
            appendSql(" then ");
            dialect.appendBooleanValueString(this, true);
            appendSql(" else ");
            dialect.appendBooleanValueString(this, false);
            appendSql(" end");
        } else {
            expression.accept(this);
        }
    }

    protected void renderCasted(Expression expression) {
        if (expression instanceof SqmParameterInterpretation) {
            expression = ((SqmParameterInterpretation) expression).getResolvedExpression();
        }
        final List<SqlAstNode> arguments = new ArrayList<>(2);
        arguments.add(expression);
        final CastTarget castTarget;
        if (expression instanceof SqlTypedMappingJdbcParameter parameter) {
            final SqlTypedMapping sqlTypedMapping = parameter.getSqlTypedMapping();
            castTarget = new CastTarget(
                    parameter.getJdbcMapping(),
                    sqlTypedMapping.getColumnDefinition(),
                    sqlTypedMapping.getLength(),
                    sqlTypedMapping.getTemporalPrecision() != null
                            ? sqlTypedMapping.getTemporalPrecision()
                            : sqlTypedMapping.getPrecision(),
                    sqlTypedMapping.getScale()
            );
        } else {
            castTarget = new CastTarget(expression.getExpressionType().getSingleJdbcMapping());
        }
        arguments.add(castTarget);
        castFunction().render(this, arguments, (ReturnableType<?>) castTarget.getJdbcMapping(), this);
    }

    @SuppressWarnings("unchecked")
    protected void renderLiteral(Literal literal, boolean castParameter) {
        assert literal.getExpressionType().getJdbcTypeCount() == 1;

        final JdbcMapping jdbcMapping = literal.getJdbcMapping();
        final JdbcLiteralFormatter<Object> literalFormatter = jdbcMapping.getJdbcLiteralFormatter();

        // If we encounter a plain literal in the select clause which has no literal formatter, we must render it as parameter
        if (literalFormatter == null) {
            parameterBinders.add(literal);
            final String marker = parameterMarkerStrategy.createMarker(
                    parameterBinders.size(),
                    literal.getJdbcMapping().getJdbcType()
            );
            final LiteralAsParameter<Object> jdbcParameter = new LiteralAsParameter<>(literal, marker);
            if (castParameter) {
                renderCasted(jdbcParameter);
            } else {
                appendSql(PARAM_MARKER);
            }
        } else {
            literalFormatter.appendJdbcLiteral(
                    this,
                    literal.getLiteralValue(),
                    dialect,
                    getWrapperOptions()
            );
        }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // FROM clause

    @Override
    public void visitFromClause(FromClause fromClause) {
        ModelPartContainer modelPart = getModelPartContainer(fromClause);
        if (modelPart instanceof AbstractEntityPersister abstractEntityPersister) {
            String[] querySpaces = (String[]) abstractEntityPersister.getQuerySpaces();
            if (querySpaces.length > 1) {
                throw new NotSupportedRuntimeException("Multiple tables not supported");
            }
            dialect.appendLiteral(this, querySpaces[0]);
        } else {
            throw new NotSupportedRuntimeException("table group's model part is not AbstractEntityPersister!");
        }
    }

    private ModelPartContainer getModelPartContainer(FromClause fromClause) {
        if (fromClause == null || fromClause.getRoots().size() != 1) {
            throw new NotSupportedRuntimeException("empty or multiple roots in from clause not supported");
        }
        TableGroup tableGroup = fromClause.getRoots().get(0);
        if (tableGroup.isVirtual()) {
            throw new NotSupportedRuntimeException("virtual table group not supported");
        }
        if (!tableGroup.getTableGroupJoins().isEmpty() || !tableGroup.getTableReferenceJoins().isEmpty()) {
            throw new NotSupportedRuntimeException("Table Joining not supported");
        }
        return tableGroup.getModelPart();
    }

    protected Predicate createRowMatchingPredicate(TableGroup dmlTargetTableGroup, String lhsAlias, String rhsAlias) {
        final String rowIdExpression = dialect.rowId(null);
        if (rowIdExpression == null) {
            final EntityIdentifierMapping identifierMapping = dmlTargetTableGroup.getModelPart()
                    .asEntityMappingType()
                    .getIdentifierMapping();
            final int jdbcTypeCount = identifierMapping.getJdbcTypeCount();
            final List<ColumnReference> targetExpressions = new ArrayList<>(jdbcTypeCount);
            final List<ColumnReference> sourceExpressions = new ArrayList<>(jdbcTypeCount);
            identifierMapping.forEachSelectable(
                    0,
                    (selectionIndex, selectableMapping) -> {
                        targetExpressions.add(new ColumnReference(
                                lhsAlias,
                                selectableMapping.getSelectionExpression(),
                                selectableMapping.isFormula(),
                                selectableMapping.getCustomReadExpression(),
                                selectableMapping.getJdbcMapping()
                        ));
                        sourceExpressions.add(new ColumnReference(
                                rhsAlias,
                                selectableMapping.getSelectionExpression(),
                                selectableMapping.isFormula(),
                                selectableMapping.getCustomReadExpression(),
                                selectableMapping.getJdbcMapping()
                        ));
                    }
            );
            return new ComparisonPredicate(
                    targetExpressions.size() == 1
                            ? targetExpressions.get(0)
                            : new SqlTuple(targetExpressions, identifierMapping),
                    ComparisonOperator.EQUAL,
                    sourceExpressions.size() == 1
                            ? sourceExpressions.get(0)
                            : new SqlTuple(sourceExpressions, identifierMapping)
            );
        } else {
            return new SelfRenderingPredicate(
                    new SelfRenderingSqlFragmentExpression(
                            lhsAlias + "." + rowIdExpression + "=" + rhsAlias + "." + rowIdExpression
                    )
            );
        }
    }

    protected void renderTableGroup(
            TableGroup tableGroup,
            Predicate predicate,
            List<TableGroupJoin> tableGroupJoinCollector) {
        // Without reference joins or nested join groups, even a real table group does not need parenthesis
        final boolean realTableGroup = tableGroup.isRealTableGroup()
                && (CollectionHelper.isNotEmpty(tableGroup.getTableReferenceJoins())
                || hasNestedTableGroupsToRender(tableGroup.getNestedTableGroupJoins()));
        if (realTableGroup) {
            appendSql(OPEN_PARENTHESIS);
        }

        final LockMode effectiveLockMode = getEffectiveLockMode(tableGroup.getSourceAlias());
        final boolean usesLockHint = renderPrimaryTableReference(tableGroup, effectiveLockMode);
        final List<TableGroupJoin> tableGroupJoins;

        if (realTableGroup) {
            // For real table groups, we collect all normal table group joins within that table group
            // The purpose of that is to render them in-order outside of the group/parenthesis
            // This is necessary for at least Derby but is also a lot easier to read
            renderTableReferenceJoins(tableGroup);
            if (tableGroupJoinCollector == null) {
                tableGroupJoins = new ArrayList<>();
                processNestedTableGroupJoins(tableGroup, tableGroupJoins);
            } else {
                tableGroupJoins = null;
                processNestedTableGroupJoins(tableGroup, tableGroupJoinCollector);
            }
            appendSql(CLOSE_PARENTHESIS);
        } else {
            tableGroupJoins = null;
        }

        if (predicate != null) {
            appendSql(" on ");
            predicate.accept(this);
        }
        if (tableGroup.isLateral() && !dialect.supportsLateral()) {
            final Predicate lateralEmulationPredicate = determineLateralEmulationPredicate(tableGroup);
            if (lateralEmulationPredicate != null) {
                if (predicate == null) {
                    appendSql(" on ");
                } else {
                    appendSql(" and ");
                }
                lateralEmulationPredicate.accept(this);
            }
        }

        if (!realTableGroup) {
            renderTableReferenceJoins(tableGroup);
            processNestedTableGroupJoins(tableGroup, tableGroupJoinCollector);
        }
        if (tableGroupJoinCollector != null) {
            tableGroupJoinCollector.addAll(tableGroup.getTableGroupJoins());
        } else {
            if (tableGroupJoins != null) {
                for (TableGroupJoin tableGroupJoin : tableGroupJoins) {
                    processTableGroupJoin(tableGroupJoin, null);
                }
            }
            processTableGroupJoins(tableGroup);
        }

        ModelPartContainer modelPart = tableGroup.getModelPart();
        if (modelPart instanceof AbstractEntityPersister) {
            String[] querySpaces = (String[]) ((AbstractEntityPersister) modelPart).getQuerySpaces();
            for (int i = 0; i < querySpaces.length; i++) {
                registerAffectedTable(querySpaces[i]);
            }
        }
        if (!usesLockHint && tableGroup.getSourceAlias() != null && LockMode.READ.lessThan(effectiveLockMode)) {
            if (forUpdate == null) {
                forUpdate = new ForUpdateClause(effectiveLockMode);
            } else {
                forUpdate.setLockMode(effectiveLockMode);
            }
            forUpdate.applyAliases(dialect.getLockRowIdentifier(effectiveLockMode), tableGroup);
        }
    }

    protected boolean hasNestedTableGroupsToRender(List<TableGroupJoin> nestedTableGroupJoins) {
        for (TableGroupJoin nestedTableGroupJoin : nestedTableGroupJoins) {
            final TableGroup joinedGroup = nestedTableGroupJoin.getJoinedGroup();
            if (!joinedGroup.isInitialized()) {
                continue;
            }
            if (joinedGroup.isVirtual()) {
                if (hasNestedTableGroupsToRender(joinedGroup.getNestedTableGroupJoins())) {
                    return true;
                }
            } else {
                return true;
            }
        }

        return false;
    }

    protected boolean renderPrimaryTableReference(TableGroup tableGroup, LockMode lockMode) {
        final TableReference tableReference = tableGroup.getPrimaryTableReference();
        if (tableReference instanceof NamedTableReference) {
            return renderNamedTableReference((NamedTableReference) tableReference, lockMode);
        }
        final DerivedTableReference derivedTableReference = (DerivedTableReference) tableReference;
        if (derivedTableReference.isLateral()) {
            if (dialect.supportsLateral()) {
                appendSql("lateral ");
            } else if (tableReference instanceof QueryPartTableReference queryPartTableReference) {
                final SelectStatement emulationStatement = stripToSelectClause(queryPartTableReference.getStatement());
                final QueryPart queryPart = queryPartTableReference.getStatement().getQueryPart();
                final QueryPart emulationQueryPart = emulationStatement.getQueryPart();
                final List<String> columnNames;
                if (queryPart instanceof QuerySpec querySpec && needsLateralSortExpressionVirtualSelections((QuerySpec) queryPart)) {
                    // One of our lateral emulations requires that sort expressions are present in the select clause
                    // when the query spec use limit/offset. So we add selections for these, if necessary
                    columnNames = new ArrayList<>(queryPartTableReference.getColumnNames());
                    final QuerySpec emulationQuerySpec = (QuerySpec) emulationQueryPart;
                    final List<SqlSelection> sqlSelections = emulationQuerySpec.getSelectClause().getSqlSelections();
                    final List<SortSpecification> sortSpecifications = queryPart.getSortSpecifications();
                    for (int i = 0; i < sortSpecifications.size(); i++) {
                        final SortSpecification sortSpecification = sortSpecifications.get(i);
                        final int sortSelectionIndex = getSortSelectionIndex(querySpec, sortSpecification);
                        if (sortSelectionIndex == -1) {
                            columnNames.add("sort_col_" + i);
                            sqlSelections.add(
                                    new SqlSelectionImpl(
                                            sqlSelections.size(),
                                            sortSpecification.getSortExpression()
                                    )
                            );
                        }
                    }
                } else {
                    columnNames = queryPartTableReference.getColumnNames();
                }
                final QueryPartTableReference emulationTableReference = new QueryPartTableReference(
                        emulationStatement,
                        tableReference.getIdentificationVariable(),
                        columnNames,
                        false,
                        sessionFactory
                );
                emulationTableReference.accept(this);
                return false;
            }
        }
        tableReference.accept(this);
        return false;
    }

    protected boolean renderNamedTableReference(NamedTableReference tableReference, LockMode lockMode) {
        appendSql(tableReference.getTableExpression());
        registerAffectedTable(tableReference);
        renderTableReferenceIdentificationVariable(tableReference);
        return false;
    }

    @Override
    public void visitValuesTableReference(ValuesTableReference tableReference) {
        append('(');
        visitValuesList(tableReference.getValuesList());
        append(')');
        renderDerivedTableReference(tableReference);
    }

    @Override
    public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
        if (tableReference.getQueryPart().isRoot()) {
            appendSql(OPEN_PARENTHESIS);
            tableReference.getStatement().accept(this);
            appendSql(CLOSE_PARENTHESIS);
        } else {
            tableReference.getStatement().accept(this);
        }
        renderDerivedTableReference(tableReference);
    }

    @Override
    public void visitFunctionTableReference(FunctionTableReference tableReference) {
        tableReference.getFunctionExpression().accept(this);
        renderDerivedTableReference(tableReference);
    }

    protected void renderDerivedTableReference(DerivedTableReference tableReference) {
        final String identificationVariable = tableReference.getIdentificationVariable();
        if (identificationVariable != null) {
            append(WHITESPACE);
            append(tableReference.getIdentificationVariable());
            final List<String> columnNames = tableReference.getColumnNames();
            append('(');
            append(columnNames.get(0));
            for (int i = 1; i < columnNames.size(); i++) {
                append(',');
                append(columnNames.get(i));
            }
            append(')');
        }
    }

    protected void renderTableReferenceIdentificationVariable(TableReference tableReference) {
        final String identificationVariable = tableReference.getIdentificationVariable();
        if (identificationVariable != null) {
            append(WHITESPACE);
            append(tableReference.getIdentificationVariable());
        }
    }

    protected void registerAffectedTable(NamedTableReference tableReference) {
        tableReference.applyAffectedTableNames(this::registerAffectedTable);
    }

    protected void registerAffectedTable(String tableExpression) {
        affectedTableNames.add(tableExpression);
    }

    protected void renderTableReferenceJoins(TableGroup tableGroup) {
        final List<TableReferenceJoin> joins = tableGroup.getTableReferenceJoins();
        if (joins == null || joins.isEmpty()) {
            return;
        }

        for (TableReferenceJoin tableJoin : joins) {
            appendSql(WHITESPACE);
            appendSql(tableJoin.getJoinType().getText());
            appendSql("join ");

            renderNamedTableReference(tableJoin.getJoinedTableReference(), LockMode.NONE);

            if (tableJoin.getPredicate() != null && !tableJoin.getPredicate().isEmpty()) {
                appendSql(" on ");
                tableJoin.getPredicate().accept(this);
            }
        }
    }

    protected void processTableGroupJoins(TableGroup source) {
        source.visitTableGroupJoins(tableGroupJoin -> processTableGroupJoin(tableGroupJoin, null));
    }

    protected void processNestedTableGroupJoins(TableGroup source, List<TableGroupJoin> tableGroupJoinCollector) {
        source.visitNestedTableGroupJoins(tableGroupJoin -> processTableGroupJoin(
                tableGroupJoin,
                tableGroupJoinCollector
        ));
    }

    protected void processTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
        final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();

        if (joinedGroup.isVirtual()) {
            processNestedTableGroupJoins(joinedGroup, tableGroupJoinCollector);
            if (tableGroupJoinCollector != null) {
                tableGroupJoinCollector.addAll(joinedGroup.getTableGroupJoins());
            } else {
                processTableGroupJoins(joinedGroup);
            }
        } else if (joinedGroup.isInitialized()) {
            renderTableGroupJoin(
                    tableGroupJoin,
                    tableGroupJoinCollector
            );
        }
        // A lazy table group, even if uninitialized, might contain table group joins
        else if (joinedGroup instanceof LazyTableGroup) {
            processNestedTableGroupJoins(joinedGroup, tableGroupJoinCollector);
            if (tableGroupJoinCollector != null) {
                tableGroupJoinCollector.addAll(joinedGroup.getTableGroupJoins());
            } else {
                processTableGroupJoins(joinedGroup);
            }
        }
    }

    protected void renderTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
        appendSql(WHITESPACE);
        appendSql(tableGroupJoin.getJoinType().getText());
        appendSql("join ");

        final Predicate predicate;
        if (tableGroupJoin.getPredicate() == null) {
            if (tableGroupJoin.getJoinType() == SqlAstJoinType.CROSS) {
                predicate = null;
            } else {
                predicate = new BooleanExpressionPredicate(new QueryLiteral<>(true, getBooleanType()));
            }
        } else {
            predicate = tableGroupJoin.getPredicate();
        }
        if (predicate != null && !predicate.isEmpty()) {
            renderTableGroup(tableGroupJoin.getJoinedGroup(), predicate, tableGroupJoinCollector);
        } else {
            renderTableGroup(tableGroupJoin.getJoinedGroup(), null, tableGroupJoinCollector);
        }
    }

    protected Predicate determineLateralEmulationPredicate(TableGroup tableGroup) {
        if (tableGroup.getPrimaryTableReference() instanceof QueryPartTableReference tableReference) {
            final List<String> columnNames = tableReference.getColumnNames();
            final List<ColumnReference> columnReferences = new ArrayList<>(columnNames.size());
            final SelectStatement statement = tableReference.getStatement();
            for (String columnName : columnNames) {
                columnReferences.add(
                        new ColumnReference(
                                tableReference,
                                columnName,
                                false,
                                null,
                                null
                        )
                );
            }

            // The following optimization only makes sense if the necessary features are supported natively
            if ((columnReferences.size() == 1 || supportsRowValueConstructorSyntax())
                    && supportsRowValueConstructorDistinctFromSyntax()) {
                // Special case for limit 1 sub-queries to avoid double nested sub-query
                // ... x(c) on x.c is not distinct from (... fetch first 1 rows only)
                if (isFetchFirstRowOnly(statement.getQueryPart())) {
                    return new ComparisonPredicate(
                            new SqlTuple(columnReferences, tableGroup.getModelPart()),
                            ComparisonOperator.NOT_DISTINCT_FROM,
                            statement
                    );
                }
            }

            // Render with exists intersect sub-query if possible as that is shorter and more efficient
            // ... x(c) on exists(select x.c intersect ...)
            if (shouldEmulateLateralWithIntersect(statement.getQueryPart())) {
                final QuerySpec lhsReferencesQuery = new QuerySpec(false);
                for (ColumnReference columnReference : columnReferences) {
                    lhsReferencesQuery.getSelectClause().addSqlSelection(new SqlSelectionImpl(columnReference));
                }
                final List<QueryPart> queryParts = new ArrayList<>(2);
                queryParts.add(lhsReferencesQuery);
                queryParts.add(statement.getQueryPart());
                return new ExistsPredicate(
                        new SelectStatement(
                                statement,
                                new QueryGroup(false, SetOperator.INTERSECT, queryParts),
                                Collections.emptyList()
                        ),
                        false,
                        getBooleanType()
                );
            }

            if (supportsNestedSubqueryCorrelation()) {
                // Double nested sub-query rendering might not work on all DBs
                // We try to avoid this as much as possible as it is not very efficient and some DBs don't like it
                // when a correlation happens in a sub-query that is not a direct child
                // ... x(c) on exists(select 1 from (...) synth_(c) where x.c distinct from synth_.c)
                final QueryPartTableGroup subTableGroup = new QueryPartTableGroup(
                        tableGroup.getNavigablePath(),
                        (TableGroupProducer) tableGroup.getModelPart(),
                        new SelectStatement(statement.getQueryPart()),
                        "synth_",
                        columnNames,
                        false,
                        true,
                        sessionFactory
                );
                final List<ColumnReference> subColumnReferences = new ArrayList<>(columnNames.size());
                for (String columnName : columnNames) {
                    subColumnReferences.add(
                            new ColumnReference(
                                    subTableGroup.getPrimaryTableReference(),
                                    columnName,
                                    false,
                                    null,
                                    null
                            )
                    );
                }
                final QuerySpec existsQuery = new QuerySpec(false, 1);
                existsQuery.getSelectClause().addSqlSelection(
                        new SqlSelectionImpl(new QueryLiteral<>(1, getIntegerType()))
                );
                existsQuery.getFromClause().addRoot(subTableGroup);
                existsQuery.applyPredicate(
                        new ComparisonPredicate(
                                new SqlTuple(columnReferences, tableGroup.getModelPart()),
                                ComparisonOperator.NOT_DISTINCT_FROM,
                                new SqlTuple(subColumnReferences, tableGroup.getModelPart())
                        )
                );

                return new ExistsPredicate(
                        new SelectStatement(
                                statement,
                                existsQuery,
                                Collections.emptyList()
                        ),
                        false,
                        getBooleanType()
                );
            }
            final QueryPart queryPart = statement.getQueryPart();
            if (queryPart instanceof QueryGroup) {
                // We can't use double nesting, but we need to add filter conditions, so fail if this is a query group
                throw new UnsupportedOperationException("Can't emulate lateral query group with limit/offset");
            }
            final QuerySpec querySpec = (QuerySpec) queryPart;

            // The last possible way to emulate lateral subqueries is to check if the correlated subquery has a result for a row.
            // Note though, that if the subquery has a limit/offset, an additional condition is needed as can be seen below
            // ... x(c) on exists(select 1 from ... and sub_.c not distinct from x.c)

            final List<Expression> subExpressions = new ArrayList<>(columnNames.size());
            for (SqlSelection sqlSelection : querySpec.getSelectClause().getSqlSelections()) {
                final Expression selectionExpression = sqlSelection.getExpression();
                final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(selectionExpression);
                if (sqlTuple == null) {
                    subExpressions.add(selectionExpression);
                } else {
                    subExpressions.addAll(sqlTuple.getExpressions());
                }
            }
            final QuerySpec existsQuery = new QuerySpec(false, querySpec.getFromClause().getRoots().size());
            existsQuery.getFromClause().getRoots().addAll(querySpec.getFromClause().getRoots());
            existsQuery.applyPredicate(querySpec.getWhereClauseRestrictions());
            existsQuery.setGroupByClauseExpressions(querySpec.getGroupByClauseExpressions());
            existsQuery.setHavingClauseRestrictions(querySpec.getHavingClauseRestrictions());
            existsQuery.getSelectClause().addSqlSelection(
                    new SqlSelectionImpl(new QueryLiteral<>(1, getIntegerType()))
            );
            existsQuery.applyPredicate(
                    new ComparisonPredicate(
                            new SqlTuple(columnReferences, tableGroup.getModelPart()),
                            ComparisonOperator.NOT_DISTINCT_FROM,
                            new SqlTuple(subExpressions, tableGroup.getModelPart())
                    )
            );

            final ExistsPredicate existsPredicate = new ExistsPredicate(
                    new SelectStatement(
                            statement,
                            existsQuery,
                            Collections.emptyList()
                    ),
                    false,
                    getBooleanType()
            );
            if (!queryPart.hasOffsetOrFetchClause()) {
                return existsPredicate;
            }
            // Emulation of lateral subqueries that use limit/offset additionally needs to compare the count of matched rows
            // ... x(c, s1) on (select count(*) from ... and sub_.s1<=x.s1) between ? and ?
            // Essentially, the subquery determines how many rows come before the current row (including that),
            // and we check if the count value is between offset and (offset+limit)

            final QuerySpec countQuery = new QuerySpec(
                    querySpec.isRoot(),
                    querySpec.getFromClause().getRoots().size()
            );
            countQuery.getFromClause().getRoots().addAll(querySpec.getFromClause().getRoots());
            countQuery.applyPredicate(querySpec.getWhereClauseRestrictions());
            countQuery.setGroupByClauseExpressions(querySpec.getGroupByClauseExpressions());
            countQuery.setHavingClauseRestrictions(querySpec.getHavingClauseRestrictions());
            countQuery.getSelectClause().addSqlSelection(
                    new SqlSelectionImpl(
                            new SelfRenderingAggregateFunctionSqlAstExpression(
                                    "count",
                                    (sqlAppender, sqlAstArguments, returnType, walker) -> sqlAppender.append("count(*)"),
                                    List.of(Star.INSTANCE),
                                    null,
                                    getIntegerType(),
                                    getIntegerType()
                            )
                    )
            );

            // Add conditions that handle the sorting of rows
            final List<SortSpecification> sortSpecifications = queryPart.getSortSpecifications();
            for (int i = 0; i < sortSpecifications.size(); i++) {
                final SortSpecification sortSpecification = sortSpecifications.get(i);
                final int sortSelectionIndex = getSortSelectionIndex(querySpec, sortSpecification);

                final ColumnReference currentRowColumnReference;
                final Expression sortExpression;
                if (sortSelectionIndex == -1) {
                    currentRowColumnReference = new ColumnReference(
                            tableReference,
                            "sort_col_" + i,
                            false,
                            null,
                            null
                    );
                    sortExpression = sortSpecification.getSortExpression();
                } else {
                    currentRowColumnReference = columnReferences.get(sortSelectionIndex);
                    sortExpression = querySpec.getSelectClause()
                            .getSqlSelections()
                            .get(sortSelectionIndex)
                            .getExpression();
                }
                // The following filter predicate will use <= for ascending and >= for descending sorting,
                // since the goal is to match all rows that come "before" the current row (including that).
                // The usual predicates are like "sortExpression <= currentRowColumnExpression",
                // but we always have to take care of null precedence handling unless we know a column is not null.
                // If nulls are to be sorted first, we can unconditionally add "... or sortExpression is null".
                // If nulls are to be sorted last, we can only add the null check if the current row column is null
                // i.e. we add "... or (currentRowColumnExpression is null and sortExpression is null)".
                final boolean isNullsFirst = isNullsFirst(sortSpecification);
                final Predicate nullHandlingPredicate;
                if (isNullsFirst) {
                    nullHandlingPredicate = new NullnessPredicate(sortExpression);
                } else {
                    nullHandlingPredicate = new Junction(
                            Junction.Nature.CONJUNCTION,
                            List.of(
                                    new NullnessPredicate(sortExpression),
                                    new NullnessPredicate(currentRowColumnReference)
                            ),
                            getBooleanType()
                    );
                }
                final ComparisonOperator comparisonOperator;
                if (sortSpecification.getSortOrder() == SortDirection.DESCENDING) {
                    comparisonOperator = ComparisonOperator.GREATER_THAN_OR_EQUAL;
                } else {
                    comparisonOperator = ComparisonOperator.LESS_THAN_OR_EQUAL;
                }
                countQuery.applyPredicate(
                        new Junction(
                                Junction.Nature.DISJUNCTION,
                                List.of(
                                        nullHandlingPredicate,
                                        new ComparisonPredicate(
                                                sortExpression,
                                                comparisonOperator,
                                                currentRowColumnReference
                                        )
                                ),
                                getBooleanType()
                        )
                );
            }

            final Expression countLower;
            final Expression countUpper;
            if (queryPart.getOffsetClauseExpression() == null) {
                countLower = new QueryLiteral<>(1, getIntegerType());
                countUpper = queryPart.getFetchClauseExpression();
            } else {
                countLower = new BinaryArithmeticExpression(
                        queryPart.getOffsetClauseExpression(),
                        BinaryArithmeticOperator.ADD,
                        new QueryLiteral<>(1, getIntegerType()),
                        getIntegerType()
                );
                countUpper = new BinaryArithmeticExpression(
                        queryPart.getOffsetClauseExpression(),
                        BinaryArithmeticOperator.ADD,
                        queryPart.getFetchClauseExpression(),
                        getIntegerType()
                );
            }
            return new Junction(
                    Junction.Nature.CONJUNCTION,
                    List.of(
                            existsPredicate,
                            new BetweenPredicate(
                                    new SelectStatement(
                                            statement,
                                            countQuery,
                                            Collections.emptyList()
                                    ),
                                    countLower,
                                    countUpper,
                                    false,
                                    getBooleanType()
                            )
                    ),
                    getBooleanType()
            );
        }
        return null;
    }

    protected boolean shouldEmulateLateralWithIntersect(QueryPart queryPart) {
        return supportsIntersect();
    }

    private boolean isNullsFirst(SortSpecification sortSpecification) {
        NullPrecedence nullPrecedence = sortSpecification.getNullPrecedence();
        if (nullPrecedence == null || nullPrecedence == NullPrecedence.NONE) {
            switch (dialect.getNullOrdering()) {
                case FIRST:
                    nullPrecedence = NullPrecedence.FIRST;
                    break;
                case LAST:
                    nullPrecedence = NullPrecedence.LAST;
                    break;
                case SMALLEST:
                    nullPrecedence = sortSpecification.getSortOrder() == SortDirection.ASCENDING
                            ? NullPrecedence.FIRST
                            : NullPrecedence.LAST;
                    break;
                case GREATEST:
                    nullPrecedence = sortSpecification.getSortOrder() == SortDirection.DESCENDING
                            ? NullPrecedence.FIRST
                            : NullPrecedence.LAST;
                    break;
            }
        }
        return nullPrecedence == NullPrecedence.FIRST;
    }

    private int getSortSelectionIndex(QuerySpec querySpec, SortSpecification sortSpecification) {
        final Expression sortExpression = sortSpecification.getSortExpression();
        if (sortExpression instanceof SqlSelectionExpression) {
            final SqlSelection selection = ((SqlSelectionExpression) sortExpression).getSelection();
            return selection.getValuesArrayPosition();
        } else {
            final List<SqlSelection> sqlSelections = querySpec.getSelectClause().getSqlSelections();
            for (int j = 0; j < sqlSelections.size(); j++) {
                final SqlSelection sqlSelection = sqlSelections.get(j);
                if (sqlSelection.getExpression() == sortExpression) {
                    return j;
                }
            }
        }
        return -1;
    }

    private boolean isFetchFirstRowOnly(QueryPart queryPart) {
        return queryPart.getFetchClauseType() == FetchClauseType.ROWS_ONLY
                && queryPart.getFetchClauseExpression() instanceof QueryLiteral<?>
                && Integer.valueOf(1)
                .equals(((QueryLiteral<?>) queryPart.getFetchClauseExpression()).getLiteralValue());
    }

    private SelectStatement stripToSelectClause(SelectStatement statement) {
        return new SelectStatement(
                statement,
                stripToSelectClause(statement.getQueryPart()),
                Collections.emptyList()
        );
    }

    private QueryPart stripToSelectClause(QueryPart queryPart) {
        if (queryPart instanceof QueryGroup) {
            return stripToSelectClause((QueryGroup) queryPart);
        } else {
            return stripToSelectClause((QuerySpec) queryPart);
        }
    }

    private QueryGroup stripToSelectClause(QueryGroup queryGroup) {
        final List<QueryPart> parts = new ArrayList<>(queryGroup.getQueryParts().size());
        for (QueryPart queryPart : queryGroup.getQueryParts()) {
            parts.add(stripToSelectClause(queryPart));
        }

        return new QueryGroup(queryGroup.isRoot(), queryGroup.getSetOperator(), parts);
    }

    private QuerySpec stripToSelectClause(QuerySpec querySpec) {
        if (querySpec.getGroupByClauseExpressions() != null && !querySpec.getGroupByClauseExpressions().isEmpty()) {
            throw new UnsupportedOperationException("Can't emulate lateral join for query spec with group by clause");
        }
        if (querySpec.getHavingClauseRestrictions() != null && !querySpec.getHavingClauseRestrictions().isEmpty()) {
            throw new UnsupportedOperationException("Can't emulate lateral join for query spec with having clause");
        }
        final QuerySpec newQuerySpec = new QuerySpec(querySpec.isRoot(), querySpec.getFromClause().getRoots().size());
        for (TableGroup root : querySpec.getFromClause().getRoots()) {
            newQuerySpec.getFromClause().addRoot(root);
        }
        for (SqlSelection selection : querySpec.getSelectClause().getSqlSelections()) {
            if (AggregateFunctionChecker.hasAggregateFunctions(selection.getExpression())) {
                throw new UnsupportedOperationException(
                        "Can't emulate lateral join for query spec with aggregate function");
            }
            newQuerySpec.getSelectClause().addSqlSelection(selection);
        }
        return newQuerySpec;
    }

    private boolean needsLateralSortExpressionVirtualSelections(QuerySpec querySpec) {
        return !((querySpec.getSelectClause()
                .getSqlSelections()
                .size() == 1 || supportsRowValueConstructorSyntax()) && supportsDistinctFromPredicate() && isFetchFirstRowOnly(
                querySpec))
                && !shouldEmulateLateralWithIntersect(querySpec)
                && !supportsNestedSubqueryCorrelation()
                && querySpec.hasOffsetOrFetchClause();
    }

    @Override
    public void visitTableGroup(TableGroup tableGroup) {
        // TableGroup and TableGroup handling should be performed as part of `#visitFromClause`...
        throw new UnsupportedOperationException(
                "This should never be invoked as org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter.visitTableGroup should handle this");
    }

    @Override
    public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
        // TableGroup and TableGroupJoin handling should be performed as part of `#visitFromClause`...
        throw new UnsupportedOperationException(
                "This should never be invoked as org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter.visitTableGroup should handle this");
    }

    @Override
    public void visitNamedTableReference(NamedTableReference tableReference) {
        // nothing to do... handled via TableGroup#render
    }

    @Override
    public void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
        // nothing to do... handled within TableGroupTableGroup#render
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Expressions

    @Override
    public void visitColumnReference(ColumnReference columnReference) {
        columnReference.appendReadExpression(this, null);
    }

    @Override
    public void visitAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression) {
        aggregateColumnWriteExpression.appendWriteExpression(
                this,
                this,
                determineColumnReferenceQualifier(aggregateColumnWriteExpression.getColumnReference())
        );
    }

    protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
        final DmlTargetColumnQualifierSupport qualifierSupport = dialect.getDmlTargetColumnQualifierSupport();
        final MutationStatement currentDmlStatement;
        final String dmlAlias;
        if (qualifierSupport == DmlTargetColumnQualifierSupport.TABLE_ALIAS
                || (currentDmlStatement = getCurrentDmlStatement()) == null
                || (dmlAlias = currentDmlStatement.getTargetTable().getIdentificationVariable()) == null
                || !dmlAlias.equals(columnReference.getQualifier())) {
            return columnReference.getQualifier();
        }
        // Qualify the column reference with the table expression also when in subqueries
        else if (qualifierSupport != DmlTargetColumnQualifierSupport.NONE || !queryPartStack.isEmpty()) {
            return getCurrentDmlStatement().getTargetTable().getTableExpression();
        } else {
            return null;
        }
    }

    @Override
    public void visitExtractUnit(ExtractUnit extractUnit) {
        appendSql(dialect.translateExtractField(extractUnit.getUnit()));
    }

    @Override
    public void visitDurationUnit(DurationUnit unit) {
        appendSql(dialect.translateDurationField(unit.getUnit()));
    }

    @Override
    public void visitFormat(Format format) {
        appendSql('\'');
        dialect.appendDatetimeFormat(this, format.getFormat());
        appendSql('\'');
    }

    @Override
    public void visitStar(Star star) {
        appendSql('*');
    }

    @Override
    public void visitTrimSpecification(TrimSpecification trimSpecification) {
        appendSql(WHITESPACE);
        appendSql(trimSpecification.getSpecification().toSqlText());
        appendSql(WHITESPACE);
    }

    @Override
    public void visitCastTarget(CastTarget castTarget) {
        appendSql(getCastTypeName(castTarget, sessionFactory.getTypeConfiguration()));
    }

    /**
     * @deprecated Use {@link #getSqlTypeName(SqlTypedMapping, TypeConfiguration)} instead
     */
    @Deprecated(forRemoval = true)
    public static String getSqlTypeName(SqlTypedMapping castTarget, SessionFactoryImplementor factory) {
        return getSqlTypeName(castTarget, factory.getTypeConfiguration());
    }

    public static String getSqlTypeName(SqlTypedMapping castTarget, TypeConfiguration typeConfiguration) {
        if (castTarget.getColumnDefinition() != null) {
            return castTarget.getColumnDefinition();
        } else {
            final Size castTargetSize = castTarget.toSize();
            final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
            final BasicType<?> expressionType = (BasicType<?>) castTarget.getJdbcMapping();
            DdlType ddlType = ddlTypeRegistry.getDescriptor(expressionType.getJdbcType().getDdlTypeCode());
            if (ddlType == null) {
                // this may happen when selecting a null value like `SELECT null from ...`
                // some dbs need the value to be cast so not knowing the real type we fall back to INTEGER
                ddlType = ddlTypeRegistry.getDescriptor(SqlTypes.INTEGER);
            }

            return ddlType.getTypeName(castTargetSize, expressionType, ddlTypeRegistry);
        }
    }

    /**
     * @deprecated Use {@link #getCastTypeName(SqlTypedMapping, TypeConfiguration)} instead
     */
    @Deprecated(forRemoval = true)
    public static String getCastTypeName(SqlTypedMapping castTarget, SessionFactoryImplementor factory) {
        return getCastTypeName(castTarget, factory.getTypeConfiguration());
    }

    public static String getCastTypeName(SqlTypedMapping castTarget, TypeConfiguration typeConfiguration) {
        if (castTarget.getColumnDefinition() != null) {
            return castTarget.getColumnDefinition();
        } else {
            final Size castTargetSize = castTarget.toSize();
            final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
            final BasicType<?> expressionType = (BasicType<?>) castTarget.getJdbcMapping();
            DdlType ddlType = ddlTypeRegistry.getDescriptor(expressionType.getJdbcType().getDdlTypeCode());
            if (ddlType == null) {
                // this may happen when selecting a null value like `SELECT null from ...`
                // some dbs need the value to be cast so not knowing the real type we fall back to INTEGER
                ddlType = ddlTypeRegistry.getDescriptor(SqlTypes.INTEGER);
            }

            return ddlType.getCastTypeName(castTargetSize, expressionType, ddlTypeRegistry);
        }
    }

    @Override
    public void visitDistinct(Distinct distinct) {
        appendSql("distinct ");
        distinct.getExpression().accept(this);
    }

    @Override
    public void visitOverflow(Overflow overflow) {
        overflow.getSeparatorExpression().accept(this);
        appendSql(" on overflow ");
        if (overflow.getFillerExpression() == null) {
            appendSql("error");
        } else {
            appendSql(" truncate ");
            overflow.getFillerExpression().accept(this);
            if (overflow.isWithCount()) {
                appendSql(" with count");
            } else {
                appendSql(" without count");
            }
        }
    }

    @Override
    public void visitParameter(JdbcParameter jdbcParameter) {
        switch (getParameterRenderingMode()) {
            case NO_UNTYPED:
            case NO_PLAIN_PARAMETER: {
                renderCasted(jdbcParameter);
                break;
            }
            case INLINE_PARAMETERS:
            case INLINE_ALL_PARAMETERS: {
                renderExpressionAsLiteral(jdbcParameter, jdbcParameterBindings);
                break;
            }
            case DEFAULT:
            default: {
                visitParameterAsParameter(jdbcParameter);
                break;
            }
        }
    }

    protected void visitParameterAsParameter(JdbcParameter jdbcParameter) {
        renderParameterAsParameter(jdbcParameter);
        parameterBinders.add(jdbcParameter.getParameterBinder());
        jdbcParameters.addParameter(jdbcParameter);
    }

    protected final void renderParameterAsParameter(JdbcParameter jdbcParameter) {
        final JdbcType jdbcType = jdbcParameter.getExpressionType().getJdbcMapping(0).getJdbcType();
        assert jdbcType != null;
        renderParameterAsParameter(parameterBinders.size() + 1, jdbcParameter);
    }

    /**
     * Renders a parameter marker for the given position
     *
     * @param jdbcParameter
     * @param position
     */
    protected void renderParameterAsParameter(int position, JdbcParameter jdbcParameter) {
        final JdbcType jdbcType = jdbcParameter.getExpressionType().getJdbcMapping(0).getJdbcType();
        assert jdbcType != null;
        final String parameterMarker = parameterMarkerStrategy.createMarker(position, jdbcType);
        jdbcType.appendWriteExpression(parameterMarker, this, dialect);
    }

    @Override
    public void render(SqlAstNode sqlAstNode, SqlAstNodeRenderingMode renderingMode) {
        SqlAstNodeRenderingMode original = this.parameterRenderingMode;
        if (original != SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS) {
            this.parameterRenderingMode = renderingMode;
        }
        try {
            sqlAstNode.accept(this);
        } finally {
            this.parameterRenderingMode = original;
        }
    }

    @Override
    public void visitTuple(SqlTuple tuple) {
        // A tuple in a values clause of an insert-select statement must be unwrapped,
        // since the assignment target is also unwrapped to the individual column references
        final boolean wrap = clauseStack.getCurrent() != Clause.VALUES;
        if (wrap) {
            appendSql(OPEN_PARENTHESIS);
        }

        renderCommaSeparated(tuple.getExpressions());

        if (wrap) {
            appendSql(CLOSE_PARENTHESIS);
        }
    }

    protected final void renderCommaSeparated(Iterable<? extends SqlAstNode> expressions) {
        String separator = NO_SEPARATOR;
        for (SqlAstNode expression : expressions) {
            appendSql(separator);
            expression.accept(this);
            separator = COMMA_SEPARATOR;
        }
    }

    protected final void renderCommaSeparatedSelectExpression(Iterable<? extends SqlAstNode> expressions) {
        String separator = NO_SEPARATOR;
        for (SqlAstNode expression : expressions) {
            final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(expression);
            if (sqlTuple != null) {
                for (Expression e : sqlTuple.getExpressions()) {
                    appendSql(separator);
                    renderSelectExpression(e);
                    separator = COMMA_SEPARATOR;
                }
            } else if (expression instanceof Expression) {
                appendSql(separator);
                renderSelectExpression((Expression) expression);
            } else {
                appendSql(separator);
                expression.accept(this);
            }
            separator = COMMA_SEPARATOR;
        }
    }

    @Override
    public void visitCollation(Collation collation) {
        appendSql(collation.getCollation());
    }

    @Override
    public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
        if (dialect.supportsOrdinalSelectItemReference()) {
            appendSql(expression.getSelection().getJdbcResultSetIndex());
        } else {
            expression.getSelection().getExpression().accept(this);
        }
    }

    @Override
    public void visitEntityTypeLiteral(EntityTypeLiteral expression) {
        appendSql(expression.getEntityTypeDescriptor().getDiscriminatorSQLValue());
    }

    @Override
    public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
        final BinaryArithmeticOperator operator = arithmeticExpression.getOperator();
        if (operator == BinaryArithmeticOperator.MODULO) {
            append("mod");
            appendSql(OPEN_PARENTHESIS);
            visitArithmeticOperand(arithmeticExpression.getLeftHandOperand());
            appendSql(',');
            visitArithmeticOperand(arithmeticExpression.getRightHandOperand());
            appendSql(CLOSE_PARENTHESIS);
        } else {
            appendSql(OPEN_PARENTHESIS);
            visitArithmeticOperand(arithmeticExpression.getLeftHandOperand());
            appendSql(arithmeticExpression.getOperator().getOperatorSqlTextString());
            visitArithmeticOperand(arithmeticExpression.getRightHandOperand());
            appendSql(CLOSE_PARENTHESIS);
        }
    }

    protected void visitArithmeticOperand(Expression expression) {
        expression.accept(this);
    }

    @Override
    public void visitDuration(Duration duration) {
        duration.getMagnitude().accept(this);
        if (!duration.getExpressionType().getJdbcMapping().getJdbcType().isInterval()) {
            // Convert to NANOSECOND because DurationJavaType requires values in that unit
            appendSql(
                    duration.getUnit().conversionFactor(NANOSECOND, dialect)
            );
        }
    }

    @Override
    public void visitConversion(Conversion conversion) {
        final Duration duration = conversion.getDuration();
        duration.getMagnitude().accept(this);
        appendSql(
                duration.getUnit().conversionFactor(
                        conversion.getUnit(), dialect
                )
        );
    }

    @Override
    public final void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
        visitCaseSearchedExpression(caseSearchedExpression, false);
    }

    protected void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression, boolean inSelect) {
        if (inSelect) {
            visitAnsiCaseSearchedExpression(caseSearchedExpression, this::renderSelectExpression);
        } else {
            visitAnsiCaseSearchedExpression(caseSearchedExpression, e -> e.accept(this));
        }
    }

    protected void visitAnsiCaseSearchedExpression(
            CaseSearchedExpression caseSearchedExpression,
            Consumer<Expression> resultRenderer) {
        appendSql("case");
        final SqlAstNodeRenderingMode original = this.parameterRenderingMode;
        for (CaseSearchedExpression.WhenFragment whenFragment : caseSearchedExpression.getWhenFragments()) {
            if (original != SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS) {
                this.parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
            }
            appendSql(" when ");
            whenFragment.getPredicate().accept(this);
            this.parameterRenderingMode = original;
            appendSql(" then ");
            resultRenderer.accept(whenFragment.getResult());
        }

        final Expression otherwise = caseSearchedExpression.getOtherwise();
        if (otherwise != null) {
            appendSql(" else ");
            resultRenderer.accept(otherwise);
        }

        appendSql(" end");
    }

    @Override
    public final void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
        visitAnsiCaseSimpleExpression(caseSimpleExpression, e -> e.accept(this));
    }

    protected void visitAnsiCaseSimpleExpression(
            CaseSimpleExpression caseSimpleExpression,
            Consumer<Expression> resultRenderer) {
        appendSql("case ");
        final SqlAstNodeRenderingMode original = this.parameterRenderingMode;
        if (original != SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS) {
            this.parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
        }
        caseSimpleExpression.getFixture().accept(this);
        for (CaseSimpleExpression.WhenFragment whenFragment : caseSimpleExpression.getWhenFragments()) {
            if (original != SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS) {
                this.parameterRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
            }
            appendSql(" when ");
            whenFragment.getCheckValue().accept(this);
            this.parameterRenderingMode = original;
            appendSql(" then ");
            resultRenderer.accept(whenFragment.getResult());
        }
        this.parameterRenderingMode = original;
        final Expression otherwise = caseSimpleExpression.getOtherwise();
        if (otherwise != null) {
            appendSql(" else ");
            resultRenderer.accept(otherwise);
        }
        appendSql(" end");
    }

    @Override
    public void visitAny(Any any) {
        appendSql("any");
        any.getSubquery().accept(this);
    }

    @Override
    public void visitEvery(Every every) {
        appendSql("all");
        every.getSubquery().accept(this);
    }

    @Override
    public void visitSummarization(Summarization every) {
        // nothing to do... handled within #renderGroupByItem
    }

    @Override
    public void visitJdbcLiteral(JdbcLiteral<?> jdbcLiteral) {
        visitLiteral(jdbcLiteral);
    }

    @Override
    public void visitQueryLiteral(QueryLiteral<?> queryLiteral) {
        visitLiteral(queryLiteral);
    }

    @Override
    public <N extends Number> void visitUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal) {
        appendSql(literal.getUnparsedLiteralValue());
    }

    private void visitLiteral(Literal literal) {
        if (literal.getLiteralValue() == null) {
            renderNull(literal);
        } else {
            renderLiteral(literal, false);
        }
    }

    protected void renderNull(Literal literal) {
        if (getParameterRenderingMode() == SqlAstNodeRenderingMode.NO_UNTYPED) {
            renderCasted(literal);
        } else {
            appendSql(SqlAppender.NULL_KEYWORD);
        }
    }

    protected void renderAsLiteral(JdbcParameter jdbcParameter, Object literalValue) {
        if (literalValue == null) {
            renderNull(new QueryLiteral<>(null, (BasicValuedMapping) jdbcParameter.getExpressionType()));
        } else {
            assert jdbcParameter.getExpressionType().getJdbcTypeCount() == 1;
            final JdbcMapping jdbcMapping = jdbcParameter.getExpressionType().getSingleJdbcMapping();
            //noinspection unchecked
            final JdbcLiteralFormatter<Object> literalFormatter = jdbcMapping.getJdbcLiteralFormatter();
            if (literalFormatter == null) {
                throw new IllegalArgumentException("Can't render parameter as literal, no literal formatter found");
            } else {
                literalFormatter.appendJdbcLiteral(
                        this,
                        literalValue,
                        dialect,
                        getWrapperOptions()
                );
            }
        }
    }

    @Override
    public void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
        if (unaryOperationExpression.getOperator() == UnaryArithmeticOperator.UNARY_PLUS) {
            appendSql(UnaryArithmeticOperator.UNARY_PLUS.getOperatorChar());
        } else {
            appendSql(UnaryArithmeticOperator.UNARY_MINUS.getOperatorChar());
        }

        unaryOperationExpression.getOperand().accept(this);
    }

    @Override
    public void visitModifiedSubQueryExpression(ModifiedSubQueryExpression expression) {
        final ModifiedSubQueryExpression.Modifier modifier = expression.getModifier();

        appendSql(modifier.getSqlName());
        appendSql(" ");
        expression.getSubQuery().accept(this);
    }

    @Override
    public void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
        // todo (6.0) render boolean expression as comparison predicate if necessary
        selfRenderingPredicate.getSelfRenderingExpression().renderToSql(this, this, getSessionFactory());
    }

    @Override
    public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
        expression.renderToSql(this, this, getSessionFactory());
    }

//	@Override
//	public void visitPluralAttribute(PluralAttributeReference pluralAttributeReference) {
//		// todo (6.0) - is this valid in the general sense?  Or specific to things like order-by rendering?
//		//		long story short... what should we do here?
//	}


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Predicates

    @Override
    public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
        // Most databases do not support boolean expressions in a predicate context, so we render `expr=true`
        booleanExpressionPredicate.getExpression().accept(this);
        appendSql('=');
        dialect.appendBooleanValueString(this, !booleanExpressionPredicate.isNegated());
    }

    @Override
    public void visitBetweenPredicate(BetweenPredicate betweenPredicate) {
        betweenPredicate.getExpression().accept(this);
        if (betweenPredicate.isNegated()) {
            appendSql(" not");
        }
        appendSql(" between ");
        betweenPredicate.getLowerBound().accept(this);
        appendSql(" and ");
        betweenPredicate.getUpperBound().accept(this);
    }

    @Override
    public void visitFilterPredicate(FilterPredicate filterPredicate) {
        // visits each fragment with " and " between them
        final List<FilterFragmentPredicate> filters = filterPredicate.getFragments();
        for (int i = 0; i < filters.size(); i++) {
            final FilterFragmentPredicate filter = filters.get(i);
            visitFilterFragmentPredicate(filter);
            if (i + 1 < filters.size()) {
                appendSql(" and ");
            }
        }
    }

    @Override
    public void visitFilterFragmentPredicate(FilterFragmentPredicate filter) {
        // process a specific filter
        final String sqlFragment = filter.getSqlFragment();

        if (filter.getParameters() == null) {
            sqlBuffer.append(sqlFragment);
            return;
        }

        int lastEnd = 0;
        for (int p = 0; p < filter.getParameters().size(); p++) {
            final FilterFragmentParameter parameter = filter.getParameters().get(p);
            lastEnd = processFilterParameter(parameter, sqlFragment, lastEnd);
        }

        if (lastEnd < sqlFragment.length()) {
            appendSql(sqlFragment.substring(lastEnd));
        }
    }

    private int processFilterParameter(FilterFragmentParameter parameter, String sqlFragment, int startPosition) {
        final String marker = ":" + parameter.getFilterName() + "." + parameter.getParameterName();
        final int markerStart = sqlFragment.indexOf(marker, startPosition);

        appendSql(sqlFragment.substring(startPosition, markerStart));

        final Object value = parameter.getValue();
        final JdbcMapping valueMapping = parameter.getValueMapping();

        if (value instanceof Iterable
                && !valueMapping.getJavaTypeDescriptor().isInstance(value)) {
            processIterableFilterParameterValue(valueMapping, ((Iterable<?>) value).iterator());
        } else {
            processSingularFilterParameterValue(valueMapping, value);
        }

        return markerStart + marker.length();
    }

    private void processSingularFilterParameterValue(JdbcMapping valueMapping, Object value) {
        visitParameterAsParameter(new FilterJdbcParameter(valueMapping, value));
    }

    private void processIterableFilterParameterValue(JdbcMapping valueMapping, Iterator<?> iterator) {
        while (iterator.hasNext()) {
            final Object element = iterator.next();
            processSingularFilterParameterValue(valueMapping, element);
            if (iterator.hasNext()) {
                appendSql(",");
            }
        }
    }

    @Override
    public void visitSqlFragmentPredicate(SqlFragmentPredicate predicate) {
        assert StringHelper.isNotEmpty(predicate.getSqlFragment());
        appendSql(predicate.getSqlFragment());
    }

    @Override
    public void visitGroupedPredicate(GroupedPredicate groupedPredicate) {
        if (groupedPredicate.isEmpty()) {
            return;
        }

        appendSql(OPEN_PARENTHESIS);
        groupedPredicate.getSubPredicate().accept(this);
        appendSql(CLOSE_PARENTHESIS);
    }

    @Override
    public void visitInListPredicate(InListPredicate inListPredicate) {
        final List<Expression> listExpressions = inListPredicate.getListExpressions();
        if (listExpressions.isEmpty()) {
            emptyInList(inListPredicate);
            return;
        }
        Function<Expression, Expression> itemAccessor = Function.identity();
        final SqlTuple lhsTuple;
        if ((lhsTuple = SqlTupleContainer.getSqlTuple(inListPredicate.getTestExpression())) != null) {
            if (lhsTuple.getExpressions().size() == 1) {
                // Special case for tuples with arity 1 as any DBMS supports scalar IN predicates
                itemAccessor = listExpression -> SqlTupleContainer.getSqlTuple(listExpression)
                        .getExpressions()
                        .get(0);
            } else if (!supportsRowValueConstructorSyntaxInInList()) {
                final ComparisonOperator comparisonOperator = inListPredicate.isNegated() ?
                        ComparisonOperator.NOT_EQUAL :
                        ComparisonOperator.EQUAL;
                // Some DBs like Oracle support tuples only for the IN subquery predicate
                if (supportsRowValueConstructorSyntaxInInSubQuery() && dialect.supportsUnionAll()) {
                    inListPredicate.getTestExpression().accept(this);
                    if (inListPredicate.isNegated()) {
                        appendSql(" not");
                    }
                    appendSql(" in (");
                    String separator = NO_SEPARATOR;
                    for (Expression expression : listExpressions) {
                        appendSql(separator);
                        renderExpressionsAsSubquery(
                                SqlTupleContainer.getSqlTuple(expression).getExpressions()
                        );
                        separator = " union all ";
                    }
                    appendSql(CLOSE_PARENTHESIS);
                } else {
                    String separator = NO_SEPARATOR;
                    appendSql(OPEN_PARENTHESIS);
                    for (Expression expression : listExpressions) {
                        appendSql(separator);
                        emulateTupleComparison(
                                lhsTuple.getExpressions(),
                                SqlTupleContainer.getSqlTuple(expression).getExpressions(),
                                comparisonOperator,
                                true
                        );
                        separator = " or ";
                    }
                    appendSql(CLOSE_PARENTHESIS);
                }
                return;
            }
        }

        int bindValueCount = listExpressions.size();
        int bindValueCountWithPadding = bindValueCount;

        int inExprLimit = dialect.getInExpressionCountLimit();

        if (getSessionFactory().getSessionFactoryOptions().inClauseParameterPaddingEnabled()) {
            bindValueCountWithPadding = addPadding(bindValueCount, inExprLimit);
        }

        final boolean parenthesis = !inListPredicate.isNegated()
                && inExprLimit > 0 && listExpressions.size() > inExprLimit;
        if (parenthesis) {
            //appendSql( OPEN_PARENTHESIS );
        }

        appendSql("{ ");

        inListPredicate.getTestExpression().accept(this);
        appendSql(": {");
        if (inListPredicate.isNegated()) {
            appendSql("\"$nin\": ");
            //appendSql( " not" );
        }
        //appendSql( " in (" );
        appendSql("\"$in\": ");
        appendSql("[");
        String separator = NO_SEPARATOR;

        final Iterator<Expression> iterator = listExpressions.iterator();
        Expression listExpression = null;
        int clauseItemNumber = 0;
        for (int i = 0; i < bindValueCountWithPadding; i++, clauseItemNumber++) {
            if (inExprLimit > 0 && inExprLimit == clauseItemNumber) {
                clauseItemNumber = 0;
                appendInClauseSeparator(inListPredicate);
                separator = NO_SEPARATOR;
            }

            if (iterator.hasNext()) { // If the iterator is exhausted, reuse the last expression for padding.
                listExpression = itemAccessor.apply(iterator.next());
            }
            // The only way for expression to be null is if listExpressions is empty,
            // but if that is the case the code takes an early exit.
            assert listExpression != null;

            appendSql(separator);
            listExpression.accept(this);
            separator = COMMA_SEPARATOR;

            // If we encounter an expression that is not a parameter or literal, we reset the inExprLimit and
            // bindValueMaxCount and just render through the in list expressions as they are without padding/splitting
            if (!(listExpression instanceof JdbcParameter || listExpression instanceof SqmParameterInterpretation || listExpression instanceof Literal)) {
                inExprLimit = 0;
                bindValueCountWithPadding = bindValueCount;
            }
        }

        appendSql("] }");
        //appendSql( CLOSE_PARENTHESIS );
        if (parenthesis) {
            //appendSql( CLOSE_PARENTHESIS );
        }
    }

    protected void emptyInList(InListPredicate inListPredicate) {
        appendSql("(");
        appendSql(inListPredicate.isNegated() ? "0" : "1");
        appendSql(" = case when ");
        inListPredicate.getTestExpression().accept(this);
        appendSql(" is not null then 0");
//		dialect.appendBooleanValueString( this, inListPredicate.isNegated() );
        appendSql(" end)");
    }

    private void appendInClauseSeparator(InListPredicate inListPredicate) {
        appendSql(CLOSE_PARENTHESIS);
        appendSql(inListPredicate.isNegated() ? " and " : " or ");
        inListPredicate.getTestExpression().accept(this);
        if (inListPredicate.isNegated()) {
            appendSql(" not");
        }
        appendSql(" in ");
        appendSql(OPEN_PARENTHESIS);
    }

    private static int addPadding(int bindValueCount, int inExprLimit) {
        int ceilingPowerOfTwo = MathHelper.ceilingPowerOfTwo(bindValueCount);
        if (inExprLimit <= 0 || ceilingPowerOfTwo <= inExprLimit) {
            return ceilingPowerOfTwo;
        }

        int numberOfInClauses = MathHelper.divideRoundingUp(bindValueCount, inExprLimit);
        int numberOfInClausesWithPadding = MathHelper.ceilingPowerOfTwo(numberOfInClauses);
        return numberOfInClausesWithPadding * inExprLimit;
    }

    @Override
    public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
        sqlBuffer.append("array_contains(");
        inArrayPredicate.getArrayParameter().accept(this);
        sqlBuffer.append(",");
        inArrayPredicate.getTestExpression().accept(this);
        sqlBuffer.append(')');
    }

    @Override
    public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
        final SqlTuple lhsTuple;
        if ((lhsTuple = SqlTupleContainer.getSqlTuple(inSubQueryPredicate.getTestExpression())) != null) {
            if (lhsTuple.getExpressions().size() == 1) {
                // Special case for tuples with arity 1 as any DBMS supports scalar IN predicates
                lhsTuple.getExpressions().get(0).accept(this);
                if (inSubQueryPredicate.isNegated()) {
                    appendSql(" not");
                }
                appendSql(" in ");
                inSubQueryPredicate.getSubQuery().accept(this);
            } else if (!supportsRowValueConstructorSyntaxInInSubQuery()) {
                emulateSubQueryRelationalRestrictionPredicate(
                        inSubQueryPredicate,
                        inSubQueryPredicate.isNegated(),
                        inSubQueryPredicate.getSubQuery(),
                        lhsTuple,
                        this::renderSelectTupleComparison,
                        ComparisonOperator.EQUAL
                );
            } else {
                inSubQueryPredicate.getTestExpression().accept(this);
                if (inSubQueryPredicate.isNegated()) {
                    appendSql(" not");
                }
                appendSql(" in ");
                inSubQueryPredicate.getSubQuery().accept(this);
            }
        } else {
            inSubQueryPredicate.getTestExpression().accept(this);
            if (inSubQueryPredicate.isNegated()) {
                appendSql(" not");
            }
            appendSql(" in ");
            inSubQueryPredicate.getSubQuery().accept(this);
        }
    }

    protected <X extends Expression> void emulateSubQueryRelationalRestrictionPredicate(
            Predicate predicate,
            boolean negated,
            SelectStatement selectStatement,
            X lhsTuple,
            SubQueryRelationalRestrictionEmulationRenderer<X> renderer,
            ComparisonOperator tupleComparisonOperator) {
        final QueryPart queryPart = selectStatement.getQueryPart();
        final QuerySpec subQuery;
        if (queryPart instanceof QuerySpec && queryPart.getFetchClauseExpression() == null
                && queryPart.getOffsetClauseExpression() == null) {
            subQuery = (QuerySpec) queryPart;
            // We can only emulate the tuple subquery predicate as exists predicate when there are no limit/offsets
            if (negated) {
                appendSql("not ");
            }

            final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
            final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
            final boolean needsSelectAliases = this.needsSelectAliases;
            try {
                this.queryPartForRowNumbering = null;
                this.queryPartForRowNumberingClauseDepth = -1;
                this.needsSelectAliases = false;
                queryPartStack.push(subQuery);
                appendSql("exists (");
                if (!subQuery.getGroupByClauseExpressions().isEmpty()
                        || subQuery.getHavingClauseRestrictions() != null) {
                    // If we have a group by or having clause, we have to move the tuple comparison emulation to the HAVING clause.
                    // Also, we need to explicitly include the selections to avoid 'invalid HAVING clause' errors
                    visitSelectClause(subQuery.getSelectClause());
                    visitFromClause(subQuery.getFromClause());
                    visitWhereClause(subQuery.getWhereClauseRestrictions());
                    visitGroupByClause(subQuery, SelectItemReferenceStrategy.EXPRESSION);

                    appendSql(" having ");
                    clauseStack.push(Clause.HAVING);
                    try {
                        renderer.renderComparison(
                                subQuery.getSelectClause().getSqlSelections(),
                                lhsTuple,
                                tupleComparisonOperator
                        );
                        final Predicate havingClauseRestrictions = subQuery.getHavingClauseRestrictions();
                        if (havingClauseRestrictions != null) {
                            appendSql(" and (");
                            havingClauseRestrictions.accept(this);
                            appendSql(CLOSE_PARENTHESIS);
                        }
                    } finally {
                        clauseStack.pop();
                    }
                } else {
                    // If we have no group by or having clause, we can move the tuple comparison emulation to the WHERE clause
                    appendSql("select 1");
                    visitFromClause(subQuery.getFromClause());
                    appendSql(" where ");
                    clauseStack.push(Clause.WHERE);
                    try {
                        renderer.renderComparison(
                                subQuery.getSelectClause().getSqlSelections(),
                                lhsTuple,
                                tupleComparisonOperator
                        );
                        final Predicate whereClauseRestrictions = subQuery.getWhereClauseRestrictions();
                        if (whereClauseRestrictions != null) {
                            appendSql(" and (");
                            whereClauseRestrictions.accept(this);
                            appendSql(CLOSE_PARENTHESIS);
                        }
                    } finally {
                        clauseStack.pop();
                    }
                }

                appendSql(CLOSE_PARENTHESIS);
            } finally {
                queryPartStack.pop();
                this.queryPartForRowNumbering = queryPartForRowNumbering;
                this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
                this.needsSelectAliases = needsSelectAliases;
            }
        } else {
            // TODO: We could use nested queries and use row numbers to emulate this
            throw new IllegalArgumentException(
                    "Can't emulate IN predicate with tuples and limit/offset or set operations: " + predicate);
        }
    }

    protected interface SubQueryRelationalRestrictionEmulationRenderer<X extends Expression> {
        void renderComparison(final List<SqlSelection> lhsExpressions, X rhsExpression, ComparisonOperator operator);
    }

    /**
     * An optimized emulation for relational tuple sub-query comparisons.
     * The idea of this method is to use limit 1 to select the max or min tuple and only compare against that.
     */
    protected void emulateQuantifiedTupleSubQueryPredicate(
            Predicate predicate,
            SelectStatement selectStatement,
            SqlTuple lhsTuple,
            ComparisonOperator tupleComparisonOperator) {
        final QueryPart queryPart = selectStatement.getQueryPart();
        final QuerySpec subQuery;
        if (queryPart instanceof QuerySpec && queryPart.getFetchClauseExpression() == null && queryPart.getOffsetClauseExpression() == null) {
            subQuery = (QuerySpec) queryPart;
            // We can only emulate the tuple subquery predicate comparing against the top element when there are no limit/offsets
            lhsTuple.accept(this);
            appendSql(tupleComparisonOperator.sqlText());

            final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
            final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
            final boolean needsSelectAliases = this.needsSelectAliases;
            try {
                this.queryPartForRowNumbering = null;
                this.queryPartForRowNumberingClauseDepth = -1;
                this.needsSelectAliases = false;
                queryPartStack.push(subQuery);
                appendSql(OPEN_PARENTHESIS);
                visitSelectClause(subQuery.getSelectClause());
                visitFromClause(subQuery.getFromClause());
                visitWhereClause(subQuery.getWhereClauseRestrictions());
                visitGroupByClause(subQuery, dialect.getGroupBySelectItemReferenceStrategy());
                visitHavingClause(subQuery);

                appendSql(" order by ");
                final List<SqlSelection> sqlSelections = subQuery.getSelectClause().getSqlSelections();
                final String order;
                if (tupleComparisonOperator == ComparisonOperator.LESS_THAN || tupleComparisonOperator == ComparisonOperator.LESS_THAN_OR_EQUAL) {
                    // Default order is asc so we don't need to specify the order explicitly
                    order = "";
                } else {
                    order = " desc";
                }
                appendSql('1');
                appendSql(order);
                for (int i = 1; i < sqlSelections.size(); i++) {
                    appendSql(COMMA_SEPARATOR_CHAR);
                    appendSql(i + 1);
                    appendSql(order);
                }
                renderFetch(
                        new QueryLiteral<>(1, getIntegerType()),
                        null,
                        FetchClauseType.ROWS_ONLY
                );
                appendSql(CLOSE_PARENTHESIS);
            } finally {
                queryPartStack.pop();
                this.queryPartForRowNumbering = queryPartForRowNumbering;
                this.queryPartForRowNumberingClauseDepth = queryPartForRowNumberingClauseDepth;
                this.needsSelectAliases = needsSelectAliases;
            }
        } else {
            // TODO: We could use nested queries and use row numbers to emulate this
            throw new IllegalArgumentException(
                    "Can't emulate in predicate with tuples and limit/offset or set operations: " + predicate);
        }
    }

    @Override
    public void visitExistsPredicate(ExistsPredicate existsPredicate) {
        appendSql("{ ");
        if (existsPredicate.isNegated()) {
            appendSql(" $not: {");
        }
        appendSql("$exists: ");
        existsPredicate.getExpression().accept(this);
        if (existsPredicate.isNegated()) {
            appendSql("} ");
        }
        appendSql(" }");
    }

    @Override
    public void visitJunction(Junction junction) {
        if (junction.isEmpty()) {
            return;
        }

        appendSql("{ ");
        final Junction.Nature nature = junction.getNature();
        if (nature == Junction.Nature.CONJUNCTION) {
            appendSql("$and: ");
        } else {
            appendSql("$or: ");
        }
        appendSql("[");
        final List<Predicate> predicates = junction.getPredicates();
        visitJunctionPredicate(nature, predicates.get(0));
        for (int i = 1; i < predicates.size(); i++) {
            appendSql(", ");
            visitJunctionPredicate(nature, predicates.get(i));
        }
        appendSql("] }");
    }

    private void visitJunctionPredicate(Junction.Nature nature, Predicate p) {
        if (p instanceof Junction junction) {
            // If we have the same nature, or if this is a disjunction and the operand is a conjunction,
            // then we don't need parenthesis, because the AND operator binds stronger
            if (nature == junction.getNature() || nature == Junction.Nature.DISJUNCTION) {
                p.accept(this);
            } else {
                appendSql(OPEN_PARENTHESIS);
                p.accept(this);
                appendSql(CLOSE_PARENTHESIS);
            }
        } else {
            p.accept(this);
        }
    }

    @Override
    public void visitLikePredicate(LikePredicate likePredicate) {
        if (likePredicate.isCaseSensitive()) {
            likePredicate.getMatchExpression().accept(this);
            if (likePredicate.isNegated()) {
                appendSql(" not");
            }
            appendSql(" like ");
            likePredicate.getPattern().accept(this);
            if (likePredicate.getEscapeCharacter() != null) {
                appendSql(" escape ");
                likePredicate.getEscapeCharacter().accept(this);
            }
        } else {
            if (dialect.supportsCaseInsensitiveLike()) {
                likePredicate.getMatchExpression().accept(this);
                if (likePredicate.isNegated()) {
                    appendSql(" not");
                }
                appendSql(WHITESPACE);
                appendSql(dialect.getCaseInsensitiveLike());
                appendSql(WHITESPACE);
                likePredicate.getPattern().accept(this);
                if (likePredicate.getEscapeCharacter() != null) {
                    appendSql(" escape ");
                    likePredicate.getEscapeCharacter().accept(this);
                }
            } else {
                renderCaseInsensitiveLikeEmulation(
                        likePredicate.getMatchExpression(),
                        likePredicate.getPattern(),
                        likePredicate.getEscapeCharacter(),
                        likePredicate.isNegated()
                );
            }
        }
    }

    protected void renderCaseInsensitiveLikeEmulation(
            Expression lhs,
            Expression rhs,
            Expression escapeCharacter,
            boolean negated) {
        //LOWER(lhs) operator LOWER(rhs)
        appendSql(dialect.getLowercaseFunction());
        appendSql(OPEN_PARENTHESIS);
        lhs.accept(this);
        appendSql(CLOSE_PARENTHESIS);
        if (negated) {
            appendSql(" not");
        }
        appendSql(" like ");
        appendSql(dialect.getLowercaseFunction());
        appendSql(OPEN_PARENTHESIS);
        rhs.accept(this);
        appendSql(CLOSE_PARENTHESIS);
        if (escapeCharacter != null) {
            appendSql(" escape ");
            escapeCharacter.accept(this);
        }
    }

    @Override
    public void visitNegatedPredicate(NegatedPredicate negatedPredicate) {
        if (negatedPredicate.isEmpty()) {
            return;
        }
        appendSql("{ $not: ");
        negatedPredicate.getPredicate().accept(this);
        appendSql(" }");
    }

    @Override
    public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
        final Expression expression = nullnessPredicate.getExpression();
        final String predicateValue = nullnessPredicate.isNegated() ? " is not null" : " is null";
        final SqlTuple tuple;
        if ((tuple = SqlTupleContainer.getSqlTuple(expression)) != null) {
            String separator = NO_SEPARATOR;
            // HQL has different semantics for the not null check on embedded attribute mappings
            // as the embeddable is not considered as null, if at least one sub-part is not null
            if (nullnessPredicate.isNegated() && expression.getExpressionType() instanceof AttributeMapping) {
                appendSql('(');
                for (Expression exp : tuple.getExpressions()) {
                    appendSql(separator);
                    exp.accept(this);
                    appendSql(predicateValue);
                    separator = " or ";
                }
                appendSql(')');
            }
            // For the is null check, and also for tuples in SQL in general,
            // the semantics is that all sub-parts must match the predicate
            else {
                for (Expression exp : tuple.getExpressions()) {
                    appendSql(separator);
                    exp.accept(this);
                    appendSql(predicateValue);
                    separator = " and ";
                }
            }
            return;
        }
        expression.accept(this);
        appendSql(predicateValue);
    }

    @Override
    public void visitThruthnessPredicate(ThruthnessPredicate thruthnessPredicate) {
        if (dialect.supportsIsTrue()) {
            thruthnessPredicate.getExpression().accept(this);
            appendSql(" is ");
            if (thruthnessPredicate.isNegated()) {
                appendSql("not ");
            }
            appendSql(thruthnessPredicate.getBooleanValue());
        } else {
            String literalTrue = dialect.toBooleanValueString(true);
            String literalFalse = dialect.toBooleanValueString(false);
            appendSql("(case ");
            thruthnessPredicate.getExpression().accept(this);
            appendSql(" when ");
            appendSql(thruthnessPredicate.getBooleanValue() ? literalTrue : literalFalse);
            appendSql(" then ");
            appendSql(thruthnessPredicate.isNegated() ? literalFalse : literalTrue);
            appendSql(" when ");
            appendSql(thruthnessPredicate.getBooleanValue() ? literalFalse : literalTrue);
            appendSql(" then ");
            appendSql(thruthnessPredicate.isNegated() ? literalTrue : literalFalse);
            appendSql(" else ");
            appendSql(thruthnessPredicate.isNegated() ? literalTrue : literalFalse);
            appendSql(" end = ");
            appendSql(literalTrue);
            appendSql(")");
        }
    }

    @Override
    public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
        // todo (6.0) : do we want to allow multi-valued parameters in a relational predicate?
        //		yes means we'd have to support dynamically converting this predicate into
        //		an IN predicate or an OR predicate
        //
        //		NOTE: JPA does not define support for multi-valued parameters here.
        //
        // If we decide to support that ^^  we should validate that *both* sides of the
        //		predicate are multi-valued parameters.  because...
        //		well... its stupid :)

        final SqlTuple lhsTuple;
        final SqlTuple rhsTuple;
        if ((lhsTuple = SqlTupleContainer.getSqlTuple(comparisonPredicate.getLeftHandExpression())) != null) {
            final Expression rhsExpression = comparisonPredicate.getRightHandExpression();
            final boolean all;
            final SelectStatement subquery;

            // Handle emulation of quantified comparison
            if (rhsExpression instanceof SelectStatement) {
                subquery = (SelectStatement) rhsExpression;
                all = true;
            } else if (rhsExpression instanceof Every) {
                subquery = ((Every) rhsExpression).getSubquery();
                all = true;
            } else if (rhsExpression instanceof Any) {
                subquery = ((Any) rhsExpression).getSubquery();
                all = false;
            } else {
                subquery = null;
                all = false;
            }

            final ComparisonOperator operator = comparisonPredicate.getOperator();
            if (lhsTuple.getExpressions().size() == 1) {
                // Special case for tuples with arity 1 as any DBMS supports scalar IN predicates
                if (subquery == null) {
                    renderComparison(
                            lhsTuple.getExpressions().get(0),
                            operator,
                            SqlTupleContainer.getSqlTuple(comparisonPredicate.getRightHandExpression())
                                    .getExpressions()
                                    .get(0)
                    );
                } else {
                    renderComparison(lhsTuple.getExpressions().get(0), operator, rhsExpression);
                }
            } else if (subquery != null && !supportsRowValueConstructorSyntaxInQuantifiedPredicates()) {
                // For quantified relational comparisons, we can do an optimized emulation
                if (!needsTupleComparisonEmulation(operator) && all) {
                    switch (operator) {
                        case LESS_THAN:
                        case LESS_THAN_OR_EQUAL:
                        case GREATER_THAN:
                        case GREATER_THAN_OR_EQUAL: {
                            emulateQuantifiedTupleSubQueryPredicate(
                                    comparisonPredicate,
                                    subquery,
                                    lhsTuple,
                                    operator
                            );
                            return;
                        }
                        case NOT_EQUAL:
                        case EQUAL:
                        case DISTINCT_FROM:
                        case NOT_DISTINCT_FROM: {
                            // For this special case, we can rely on scalar subquery handling, given that the subquery fetches only one row
                            if (isFetchFirstRowOnly(subquery.getQueryPart())) {
                                renderComparison(lhsTuple, operator, subquery);
                                return;
                            }
                        }
                    }
                }
                emulateSubQueryRelationalRestrictionPredicate(
                        comparisonPredicate,
                        all,
                        subquery,
                        lhsTuple,
                        this::renderSelectTupleComparison,
                        all ? operator.negated() : operator
                );
            } else if (needsTupleComparisonEmulation(operator)) {
                rhsTuple = SqlTupleContainer.getSqlTuple(rhsExpression);
                assert rhsTuple != null;
                // Some DBs like Oracle support tuples only for the IN subquery predicate
                if ((operator == ComparisonOperator.EQUAL || operator == ComparisonOperator.NOT_EQUAL) && supportsRowValueConstructorSyntaxInInSubQuery()) {
                    comparisonPredicate.getLeftHandExpression().accept(this);
                    if (operator == ComparisonOperator.NOT_EQUAL) {
                        appendSql(" not");
                    }
                    appendSql(" in (");
                    renderExpressionsAsSubquery(rhsTuple.getExpressions());
                    appendSql(CLOSE_PARENTHESIS);
                } else {
                    emulateTupleComparison(
                            lhsTuple.getExpressions(),
                            rhsTuple.getExpressions(),
                            operator,
                            true
                    );
                }
            } else {
                renderComparison(comparisonPredicate.getLeftHandExpression(), operator, rhsExpression);
            }
        } else if ((rhsTuple = SqlTupleContainer.getSqlTuple(comparisonPredicate.getRightHandExpression())) != null) {
            final Expression lhsExpression = comparisonPredicate.getLeftHandExpression();

            if (lhsExpression instanceof SqlTupleContainer
                    || lhsExpression instanceof SelectStatement && ((SelectStatement) lhsExpression).getQueryPart() instanceof QueryGroup) {
                if (rhsTuple.getExpressions().size() == 1) {
                    // Special case for tuples with arity 1 as any DBMS supports scalar IN predicates
                    renderComparison(
                            lhsExpression,
                            comparisonPredicate.getOperator(),
                            rhsTuple.getExpressions().get(0)
                    );
                } else if (!needsTupleComparisonEmulation(comparisonPredicate.getOperator())) {
                    renderComparison(
                            lhsExpression,
                            comparisonPredicate.getOperator(),
                            comparisonPredicate.getRightHandExpression()
                    );
                } else {
                    emulateSubQueryRelationalRestrictionPredicate(
                            comparisonPredicate,
                            false,
                            (SelectStatement) lhsExpression,
                            rhsTuple,
                            this::renderSelectTupleComparison,
                            // Since we switch the order of operands, we have to invert the operator
                            comparisonPredicate.getOperator().invert()
                    );
                }
            } else {
                throw new IllegalStateException(
                        "Unsupported tuple comparison combination. LHS is neither a tuple nor a tuple subquery but RHS is a tuple: " + comparisonPredicate);
            }
        } else {
            renderComparison(
                    comparisonPredicate.getLeftHandExpression(),
                    comparisonPredicate.getOperator(),
                    comparisonPredicate.getRightHandExpression()
            );
        }
    }

    private boolean needsTupleComparisonEmulation(ComparisonOperator operator) {
        if (!supportsRowValueConstructorSyntax()) {
            return true;
        }
        switch (operator) {
            case LESS_THAN:
            case LESS_THAN_OR_EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL:
                return !supportsRowValueConstructorGtLtSyntax();
            case DISTINCT_FROM:
            case NOT_DISTINCT_FROM:
                return !supportsRowValueConstructorDistinctFromSyntax();
        }
        return false;
    }

    /**
     * Is this SQL dialect known to support some kind of distinct from predicate.
     * <p>
     * Basically, does it support syntax like
     * {@code ... where FIRST_NAME IS DISTINCT FROM LAST_NAME}
     *
     * @return True if this SQL dialect is known to support some kind of distinct from predicate; false otherwise
     */
    protected boolean supportsDistinctFromPredicate() {
        return dialect.supportsDistinctFromPredicate();
    }

    /**
     * Is this dialect known to support what ANSI-SQL terms "row value
     * constructor" syntax; sometimes called tuple syntax.
     * <p>
     * Basically, does it support syntax like
     * {@code ... where (FIRST_NAME, LAST_NAME) = ('Steve', 'Ebersole') ...}
     *
     * @return True if this SQL dialect is known to support "row value
     * constructor" syntax; false otherwise.
     */
    protected boolean supportsRowValueConstructorSyntax() {
        return true;
    }

    /**
     * Is this dialect known to support what ANSI-SQL terms "row value
     * constructor" syntax; sometimes called tuple syntax with <code>&lt;</code>, <code>&gt;</code>, <code>&le;</code>
     * and <code>&ge;</code> operators.
     * <p>
     * Basically, does it support syntax like
     * {@code ... where (FIRST_NAME, LAST_NAME) &lt; ('Steve', 'Ebersole') ...}
     *
     * @return True if this SQL dialect is known to support "row value
     * constructor" syntax with relational comparison operators; false otherwise.
     */
    protected boolean supportsRowValueConstructorGtLtSyntax() {
        return supportsRowValueConstructorSyntax();
    }

    /**
     * Is this dialect known to support what ANSI-SQL terms "row value
     * constructor" syntax; sometimes called tuple syntax with <code>is distinct from</code>
     * and <code>is not distinct from</code> operators.
     * <p>
     * Basically, does it support syntax like
     * {@code ... where (FIRST_NAME, LAST_NAME) is distinct from ('Steve', 'Ebersole') ...}
     *
     * @return True if this SQL dialect is known to support "row value
     * constructor" syntax with distinct from comparison operators; false otherwise.
     */
    protected boolean supportsRowValueConstructorDistinctFromSyntax() {
        return supportsRowValueConstructorSyntax() && supportsDistinctFromPredicate();
    }

    /**
     * Is this dialect known to support what ANSI-SQL terms "row value
     * constructor" syntax; sometimes called tuple syntax with quantified predicates.
     * <p>
     * Basically, does it support syntax like
     * {@code ... where (FIRST_NAME, LAST_NAME) = ALL (select ...) ...}
     *
     * @return True if this SQL dialect is known to support "row value
     * constructor" syntax with quantified predicates; false otherwise.
     */
    protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
        return true;
    }

    /**
     * If the dialect supports {@link #supportsRowValueConstructorSyntax() row values},
     * does it offer such support in IN lists as well?
     * <p>
     * For example, {@code ... where (FIRST_NAME, LAST_NAME) IN ( (?, ?), (?, ?) ) ...}
     *
     * @return True if this SQL dialect is known to support "row value
     * constructor" syntax in the IN list; false otherwise.
     */
    protected boolean supportsRowValueConstructorSyntaxInInList() {
        return true;
    }

    /**
     * If the dialect supports {@link #supportsRowValueConstructorSyntax() row values},
     * does it offer such support in IN subqueries as well?
     * <p>
     * For example, {@code ... where (FIRST_NAME, LAST_NAME) IN ( select ... ) ...}
     *
     * @return True if this SQL dialect is known to support "row value
     * constructor" syntax in the IN subqueries; false otherwise.
     */
    protected boolean supportsRowValueConstructorSyntaxInInSubQuery() {
        return supportsRowValueConstructorSyntaxInInList();
    }

    /**
     * If the dialect supports using joins in mutation statement subquery
     * that could also use columns from the mutation target table
     */
    protected boolean supportsJoinInMutationStatementSubquery() {
        return true;
    }

    /**
     * Some databases require a bit of syntactic noise when
     * there are no tables in the from clause.
     *
     * @return the SQL equivalent to Oracle's {@code from dual}.
     * @deprecated Use {@link #getDual()} instead
     */
    @Deprecated(forRemoval = true)
    protected String getFromDual() {
        return " from " + getDual() + " d_";
    }

    /**
     * Returns a table expression that has one row.
     *
     * @return the SQL equivalent to Oracle's {@code dual}.
     */
    protected String getDual() {
        return "(values(0))";
    }

    protected String getFromDualForSelectOnly() {
        return "";
    }

    protected enum LockStrategy {
        CLAUSE,
        FOLLOW_ON,
        NONE
    }

    protected static class ForUpdateClause {
        private LockMode lockMode;
        private int timeoutMillis = LockOptions.WAIT_FOREVER;
        private Map<String, String[]> keyColumnNames;
        private Map<String, String> aliases;

        public ForUpdateClause(LockMode lockMode) {
            this.lockMode = lockMode;
        }

        public ForUpdateClause() {
            this.lockMode = LockMode.NONE;
        }

        public void applyAliases(RowLockStrategy lockIdentifier, QuerySpec querySpec) {
            if (lockIdentifier != RowLockStrategy.NONE) {
                querySpec.getFromClause().visitTableGroups(tableGroup -> applyAliases(lockIdentifier, tableGroup));
            }
        }

        public void applyAliases(RowLockStrategy lockIdentifier, TableGroup tableGroup) {
            if (aliases != null && lockIdentifier != RowLockStrategy.NONE) {
                final String tableAlias = tableGroup.getPrimaryTableReference().getIdentificationVariable();
                if (aliases.containsKey(tableGroup.getSourceAlias())) {
                    addAlias(tableGroup.getSourceAlias(), tableAlias);
                    if (lockIdentifier == RowLockStrategy.COLUMN) {
                        addKeyColumnNames(tableGroup);
                    }
                }
            }
        }

        public LockMode getLockMode() {
            return lockMode;
        }

        public void setLockMode(LockMode lockMode) {
            if (this.lockMode != LockMode.NONE && lockMode != this.lockMode) {
                throw new QueryException("Mixed LockModes");
            }
            this.lockMode = lockMode;
        }

        public void addKeyColumnNames(TableGroup tableGroup) {
            final String[] keyColumnNames = determineKeyColumnNames(tableGroup.getModelPart());
            if (keyColumnNames == null) {
                throw new IllegalArgumentException("Can't lock table group: " + tableGroup);
            }
            addKeyColumnNames(
                    tableGroup.getSourceAlias(),
                    tableGroup.getPrimaryTableReference().getIdentificationVariable(),
                    keyColumnNames
            );
        }

        private String[] determineKeyColumnNames(ModelPart modelPart) {
            if (modelPart instanceof Loadable) {
                return ((Loadable) modelPart).getIdentifierColumnNames();
            } else if (modelPart instanceof PluralAttributeMapping) {
                return ((PluralAttributeMapping) modelPart).getCollectionDescriptor().getKeyColumnAliases(null);
            } else if (modelPart instanceof EntityAssociationMapping) {
                return determineKeyColumnNames(((EntityAssociationMapping) modelPart).getAssociatedEntityMappingType());
            }
            return null;
        }

        private void addKeyColumnNames(String alias, String tableAlias, String[] keyColumnNames) {
            if (this.keyColumnNames == null) {
                this.keyColumnNames = new HashMap<>();
            }
            this.keyColumnNames.put(tableAlias, keyColumnNames);
        }

        public boolean hasAlias(String alias) {
            return aliases != null && aliases.containsKey(alias);
        }

        private void addAlias(String alias, String tableAlias) {
            if (aliases == null) {
                aliases = new HashMap<>();
            }
            aliases.put(alias, tableAlias);
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public boolean hasAliases() {
            return aliases != null;
        }

        public void appendAliases(SqlAppender appender) {
            if (aliases == null) {
                return;
            }
            if (keyColumnNames != null) {
                boolean first = true;
                for (String tableAlias : aliases.values()) {
                    final String[] keyColumns = keyColumnNames.get(tableAlias); //use the id column alias
                    if (keyColumns == null) {
                        throw new IllegalArgumentException("alias not found: " + tableAlias);
                    }
                    for (String keyColumn : keyColumns) {
                        if (first) {
                            first = false;
                        } else {
                            appender.appendSql(',');
                        }
                        appender.appendSql(tableAlias);
                        appender.appendSql('.');
                        appender.appendSql(keyColumn);
                    }
                }
            } else {
                boolean first = true;
                for (String tableAlias : aliases.values()) {
                    if (first) {
                        first = false;
                    } else {
                        appender.appendSql(',');
                    }
                    appender.appendSql(tableAlias);
                }
            }
        }

        public String getAliases() {
            if (aliases == null) {
                return null;
            }
            return aliases.toString();
        }

        public void merge(LockOptions lockOptions) {
            if (lockOptions != null) {
                LockMode upgradeType = LockMode.NONE;
                if (lockOptions.getAliasLockCount() == 0) {
                    upgradeType = lockOptions.getLockMode();
                } else {
                    for (Map.Entry<String, LockMode> entry : lockOptions.getAliasSpecificLocks()) {
                        final LockMode lockMode = entry.getValue();
                        if (LockMode.READ.lessThan(lockMode)) {
                            addAlias(entry.getKey(), null);
                            if (upgradeType != LockMode.NONE && lockMode != upgradeType) {
                                throw new QueryException("Mixed LockModes");
                            }
                            upgradeType = lockMode;
                        }
                    }
                }
                lockMode = upgradeType;
                timeoutMillis = lockOptions.getTimeOut();
            }
        }
    }

    private T translateTableMutation(TableMutation<?> mutation) {
        mutation.accept(this);
        //noinspection unchecked
        return (T) mutation.createMutationOperation(getSql(), parameterBinders);
    }

    @Override
    public void visitStandardTableInsert(TableInsertStandard tableInsert) {
        getCurrentClauseStack().push(Clause.INSERT);
        try {
            renderInsertInto(tableInsert);
            if (tableInsert.getNumberOfReturningColumns() > 0) {
                visitReturningColumns(tableInsert::getReturningColumns);
            }
        } finally {
            getCurrentClauseStack().pop();
        }
    }

    private void renderInsertInto(TableInsertStandard tableInsert) {
        if (tableInsert.getNumberOfValueBindings() == 0) {
            renderInsertIntoNoColumns(tableInsert);
            return;
        }
        appendSql("{ insert: ");
        renderIntoIntoAndTable(tableInsert);
        appendSql(", documents: [ { ");

        tableInsert.forEachValueBinding((columnPosition, columnValueBinding) -> {
            if (columnPosition != 0) {
                appendSql(", ");
            }
            appendSql(columnValueBinding.getColumnReference().getColumnExpression());
            appendSql(": ");
            columnValueBinding.getValueExpression().accept(this);
        });

        appendSql(" } ]");
        //applySqlComment( tableInsert.getMutationComment() );
        appendSql(" }");
    }

    /**
     * Renders the {@code insert into <table name>} portion of an insert
     */
    protected void renderIntoIntoAndTable(TableInsertStandard tableInsert) {
        dialect.appendLiteral(this, tableInsert.getMutatingTable().getTableName());
        registerAffectedTable(tableInsert.getMutatingTable().getTableName());
    }

    /**
     * Handle rendering an insert with no columns
     */
    protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
        renderIntoIntoAndTable(tableInsert);
        sqlBuffer.append(dialect.getNoColumnsInsertString());
    }

    @Override
    public void visitCustomTableInsert(TableInsertCustomSql tableInsert) {
        assert sqlBuffer.toString().isEmpty();
        sqlBuffer.append(tableInsert.getCustomSql());

        tableInsert.forEachParameter(this::applyParameter);
    }

    @Override
    public void visitStandardTableUpdate(TableUpdateStandard tableUpdate) {
        getCurrentClauseStack().push(Clause.UPDATE);
        try {
            appendSql('{');
            {
                visitTableUpdate(tableUpdate);
            }
            appendSql(" }");
        } finally {
            getCurrentClauseStack().pop();
        }
    }

    @Override
    public void visitOptionalTableUpdate(OptionalTableUpdate tableUpdate) {
        getCurrentClauseStack().push(Clause.UPDATE);
        try {
            visitTableUpdate(tableUpdate);
        } finally {
            getCurrentClauseStack().pop();
        }
    }

    private void visitTableUpdate(RestrictedTableMutation<? extends MutationOperation> tableUpdate) {
        appendSql(" update: ");
        dialect.appendLiteral(this, tableUpdate.getMutatingTable().getTableName());
        registerAffectedTable(tableUpdate.getMutatingTable().getTableName());

        appendSql(", updates: [ {");
        {
            getCurrentClauseStack().push(Clause.WHERE);
            try {
                appendSql(" q:");
                int predicates = tableUpdate.getNumberOfKeyBindings() + tableUpdate.getNumberOfOptimisticLockBindings();
                boolean hasWhereFragment =
                        tableUpdate instanceof TableUpdateStandard tableUpdateStandard && tableUpdateStandard.getWhereFragment() != null;
                if (hasWhereFragment) {
                    predicates++;
                }
                if (predicates == 0) {
                    appendSql("{}");
                } else {
                    if (predicates > 1) {
                        appendSql(" { $and: [");
                    }
                    tableUpdate.forEachKeyBinding((position, columnValueBinding) -> {
                        if (position == 0) {
                            appendSql(' ');
                        } else {
                            appendSql(", ");
                        }
                        appendSql("{ ");
                        appendSql(columnValueBinding.getColumnReference().getColumnExpression());
                        appendSql(": { $eq: ");
                        columnValueBinding.getValueExpression().accept(this);
                        appendSql(" } }");
                    });

                    if (tableUpdate.getNumberOfOptimisticLockBindings() > 0) {
                        tableUpdate.forEachOptimisticLockBinding((position, columnValueBinding) -> {
                            appendSql(", { ");
                            appendSql(columnValueBinding.getColumnReference().getColumnExpression());
                            appendSql(": { $eq: ");
                            if (columnValueBinding.getValueExpression() == null) {
                                appendSql("null");
                            } else {
                                columnValueBinding.getValueExpression().accept(this);
                            }
                            appendSql(" } }");
                        });
                    }

                    if (hasWhereFragment) {
                        appendSql(", ");
                        appendSql(((TableUpdateStandard) tableUpdate).getWhereFragment());
                    }
                    if (predicates > 1) {
                        appendSql(" ] }");
                    }
                }
            } finally {
                getCurrentClauseStack().pop();
            }

            appendSql(", u: { $set: {");
            {
                getCurrentClauseStack().push(Clause.SET);
                try {
                    tableUpdate.forEachValueBinding((columnPosition, columnValueBinding) -> {
                        if (columnPosition == 0) {
                            appendSql(' ');
                        } else {
                            appendSql(", ");
                        }
                        sqlBuffer.append(columnValueBinding.getColumnReference().getColumnExpression());
                        sqlBuffer.append(": ");
                        columnValueBinding.getValueExpression().accept(this);
                    });
                } finally {
                    getCurrentClauseStack().pop();
                }
                appendSql(" } }");
            }
        }
        appendSql(" } ]");
    }

    @Override
    public void visitCustomTableUpdate(TableUpdateCustomSql tableUpdate) {
        assert sqlBuffer.toString().isEmpty();
        sqlBuffer.append(tableUpdate.getCustomSql());

        tableUpdate.forEachParameter(this::applyParameter);
    }

    @Override
    public void visitStandardTableDelete(TableDeleteStandard tableDelete) {
        getCurrentClauseStack().push(Clause.DELETE);
        try {
            appendSql("{ delete: ");
            dialect.appendLiteral(this, tableDelete.getMutatingTable().getTableName());
            registerAffectedTable(tableDelete.getMutatingTable().getTableName());

            getCurrentClauseStack().push(Clause.WHERE);
            try {
                appendSql(", deletes: [");

                tableDelete.forEachKeyBinding((columnPosition, columnValueBinding) -> {
                    appendSql(" { q: { ");
                    appendSql(columnValueBinding.getColumnReference().getColumnExpression());
                    appendSql(": { $eq: ");
                    columnValueBinding.getValueExpression().accept(this);
                    appendSql(" } }, limit: 0 }");
                    if (columnPosition < tableDelete.getNumberOfKeyBindings() - 1) {
                        appendSql(", ");
                    }
                });

                if (tableDelete.getNumberOfOptimisticLockBindings() > 0) {
                    appendSql(", ");

                    tableDelete.forEachOptimisticLockBinding((columnPosition, columnValueBinding) -> {
                        appendSql(" { q: { ");
                        appendSql(columnValueBinding.getColumnReference().getColumnExpression());
                        appendSql(": { $eq: ");
                        columnValueBinding.getValueExpression().accept(this);
                        appendSql(" } }, limit: 0 }");
                        if (columnPosition < tableDelete.getNumberOfOptimisticLockBindings() - 1) {
                            appendSql(", ");
                        }
                    });
                }

                if (tableDelete.getWhereFragment() != null) {
                    appendSql(", { q: ");
                    appendSql(tableDelete.getWhereFragment());
                    appendSql(" }");
                }
            } finally {
                appendSql(" ]");
                //applySqlComment( tableDelete.getMutationComment() );
                getCurrentClauseStack().pop();
            }
        } finally {
            appendSql(" }");
            getCurrentClauseStack().pop();
        }
    }

    @Override
    public void visitCustomTableDelete(TableDeleteCustomSql tableDelete) {
        assert sqlBuffer.toString().isEmpty();
        sqlBuffer.append(tableDelete.getCustomSql());

        tableDelete.forEachParameter(this::applyParameter);
    }

    protected void applyParameter(ColumnValueParameter parameter) {
        assert parameter != null;
        parameterBinders.add(parameter.getParameterBinder());
        jdbcParameters.addParameter(parameter);
    }

    @Override
    public void visitColumnWriteFragment(ColumnWriteFragment columnWriteFragment) {
        // if there are no parameters or if we are using the standard parameter renderer
        //		- the rendering is pretty simple
        if (CollectionHelper.isEmpty(columnWriteFragment.getParameters())
                || ParameterMarkerStrategyStandard.isStandardRenderer(parameterMarkerStrategy)) {
            simpleColumnWriteFragmentRendering(columnWriteFragment);
            return;
        }

        // otherwise, render the fragment using the custom parameter renderer
        final String sqlFragment = columnWriteFragment.getFragment();
        int lastEnd = 0;

        for (ColumnValueParameter parameter : columnWriteFragment.getParameters()) {
            final int markerStart = sqlFragment.indexOf('?', lastEnd);

            // append the part of the fragment from the last-end position (start of string for first pass)
            // to the index of the parameter marker
            appendSql(sqlFragment.substring(lastEnd, markerStart));

            // render the parameter marker and register the parameter handling
            visitParameterAsParameter(parameter);

            lastEnd = markerStart + 1;
        }

        if (lastEnd < sqlFragment.length()) {
            appendSql(sqlFragment.substring(lastEnd));
        }
    }

    protected void simpleColumnWriteFragmentRendering(ColumnWriteFragment columnWriteFragment) {
        appendSql(columnWriteFragment.getFragment());

        for (ColumnValueParameter parameter : columnWriteFragment.getParameters()) {
            parameterBinders.add(parameter.getParameterBinder());
            jdbcParameters.addParameter(parameter);
        }
    }

    private String getMongodbOperatorText(ComparisonOperator operator) {
        return switch (operator) {
            case EQUAL -> "$eq";
            case NOT_EQUAL -> "$ne";
            case LESS_THAN -> "$lt";
            case LESS_THAN_OR_EQUAL -> "$lte";
            case GREATER_THAN -> "$gt";
            case GREATER_THAN_OR_EQUAL -> "gte";
            default -> throw new NotSupportedRuntimeException("unsupported operator: " + operator.name());
        };
    }
}