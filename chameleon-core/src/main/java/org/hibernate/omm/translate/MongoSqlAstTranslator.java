package org.hibernate.omm.translate;

import com.mongodb.lang.Nullable;
import org.bson.BsonDocument;
import org.bson.json.JsonWriter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.omm.exception.NotSupportedRuntimeException;
import org.hibernate.omm.exception.NotYetImplementedException;
import org.hibernate.omm.translate.translator.CollectionNameAndJoinStages;
import org.hibernate.omm.translate.translator.mongoast.*;
import org.hibernate.omm.translate.translator.mongoast.expressions.AstExpression;
import org.hibernate.omm.translate.translator.mongoast.expressions.AstFieldPathExpression;
import org.hibernate.omm.translate.translator.mongoast.expressions.AstFormulaExpression;
import org.hibernate.omm.translate.translator.mongoast.filters.*;
import org.hibernate.omm.translate.translator.mongoast.stages.*;
import org.hibernate.omm.util.CollectionUtil;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.SortDirection;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.*;
import org.hibernate.sql.ast.tree.from.*;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.*;
import org.hibernate.sql.ast.tree.select.*;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQueryInsertImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.spi.*;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.internal.*;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;

import java.io.StringWriter;
import java.util.*;

public class MongoSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstWalker
        implements MqlAstWalker, SqlAstTranslator<T> {

    private static class PathTracker {
        private final Map<String, String> pathByQualifier = new HashMap<>();

        @Nullable
        private String rootQualifier;

        public void setRootQualifier(final String rootQualifier) {
            this.rootQualifier = rootQualifier;
        }

        public @Nullable String getRootQualifier() {
            return rootQualifier;
        }

        public void trackPath(final String sourceQualifier, final String targetQualifier) {
            var sourcePath = pathByQualifier.get(sourceQualifier);
            if (sourcePath != null) {
                pathByQualifier.put(targetQualifier, sourcePath + "." + targetQualifier);
            } else {
                pathByQualifier.put(targetQualifier, targetQualifier);
            }
        }

        public String renderColumnReference(final ColumnReference columnReference) {
            var path = pathByQualifier.get(columnReference.getQualifier());
            String result;
            if (path == null) {
                if (columnReference.isColumnExpressionFormula()) {
                    return columnReference.getColumnExpression().replace(columnReference.getQualifier() + ".", "");
                } else {
                    result = columnReference.getColumnExpression();
                }
            } else {
                result = path + "." + columnReference.getColumnExpression();
            }
            return "$" + result;
        }
    }

    private final PathTracker pathTracker = new PathTracker();

    private final SessionFactoryImplementor sessionFactory;

    private final Stack<Clause> clauseStack = new StandardStack<>(Clause.class);
    private final Stack<QueryPart> queryPartStack = new StandardStack<>(QueryPart.class);
    private final Stack<Statement> statementStack = new StandardStack<>(Statement.class);

    private final Set<String> affectedTableNames = new HashSet<>();

    private JdbcParameterBindings jdbcParameterBindings;
    private final List<JdbcParameterBinder> parameterBinders = new ArrayList<>();
    private final JdbcParametersImpl jdbcParameters = new JdbcParametersImpl();
    private Map<JdbcParameter, JdbcParameterBinding> appliedParameterBindings = Collections.emptyMap();

    private Limit limit;
    private JdbcParameter offsetParameter;
    private JdbcParameter limitParameter;

    private final ParameterMarkerStrategy parameterMarkerStrategy;

    @Nullable
    private Predicate additionalWherePredicate;

    private transient BasicType<Integer> integerType;
    private transient BasicType<Boolean> booleanType;

    public MongoSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
        this.sessionFactory = sessionFactory;
        final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
        this.statementStack.push(statement);
        this.parameterMarkerStrategy = jdbcServices.getParameterMarkerStrategy();
    }

    @Override
    public SessionFactoryImplementor getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public void render(SqlAstNode sqlAstNode, SqlAstNodeRenderingMode renderingMode) {
        // TODO
    }

    @Override
    public boolean supportsFilterClause() {
        return false; // TODO investigate
    }

    @Override
    public QueryPart getCurrentQueryPart() {
        return queryPartStack.getCurrent();
    }

    @Override
    public Stack<Clause> getCurrentClauseStack() {
        return this.clauseStack;
    }

    private void addAdditionalWherePredicate(Predicate predicate) {
        additionalWherePredicate = Predicate.combinePredicates(additionalWherePredicate, predicate);
    }

    @Override
    public Set<String> getAffectedTableNames() {
        return affectedTableNames;
    }

    private void registerAffectedTable(String tableExpression) {
        affectedTableNames.add(tableExpression);
    }

    @Override
    public T translate(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
        try {
            this.jdbcParameterBindings = jdbcParameterBindings;

            final Statement statement = statementStack.pop();

            if (statement instanceof TableMutation) {
                return translateTableMutation((TableMutation<?>) statement);
            }

            this.limit = queryOptions.getLimit() == null
                    ? null
                    : queryOptions.getLimit().makeCopy();
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

    private JdbcOperation translateSelect(SelectStatement selectStatement) {
        var root = (AstNode) renderSelectStatement(selectStatement);

        var rowsToSkip = getRowsToSkip(selectStatement, jdbcParameterBindings);
        return new JdbcOperationQuerySelect(
                getMql(root),
                parameterBinders,
                buildJdbcValuesMappingProducer(selectStatement),
                affectedTableNames,
                rowsToSkip,
                getMaxRows(selectStatement, jdbcParameterBindings, rowsToSkip),
                appliedParameterBindings,
                JdbcLockStrategy.NONE,
                offsetParameter,
                limitParameter);
    }

    private JdbcValuesMappingProducer buildJdbcValuesMappingProducer(SelectStatement selectStatement) {
        return getSessionFactory()
                .getFastSessionServices()
                .getJdbcValuesMappingProducerProvider()
                .buildMappingProducer(selectStatement, getSessionFactory());
    }

    private int getRowsToSkip(SelectStatement sqlAstSelect, JdbcParameterBindings jdbcParameterBindings) {
        if (hasLimit()) {
            if (offsetParameter != null && needsRowsToSkip()) {
                return interpretExpression(offsetParameter, jdbcParameterBindings);
            }
        } else {
            final Expression offsetClauseExpression =
                    sqlAstSelect.getQueryPart().getOffsetClauseExpression();
            if (offsetClauseExpression != null && needsRowsToSkip()) {
                return interpretExpression(offsetClauseExpression, jdbcParameterBindings);
            }
        }
        return 0;
    }

    private int getMaxRows(
            SelectStatement sqlAstSelect, JdbcParameterBindings jdbcParameterBindings, int rowsToSkip) {
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

    private <R> R interpretExpression(Expression expression, JdbcParameterBindings jdbcParameterBindings) {
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
            return (R) getParameterBindValue(
                    (JdbcParameter) ((SqmParameterInterpretation) expression).getResolvedExpression());
        }
        throw new UnsupportedOperationException("Can't interpret expression: " + expression);
    }

    private void addAppliedParameterBinding(JdbcParameter parameter, JdbcParameterBinding binding) {
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

    private Object getParameterBindValue(JdbcParameter parameter) {
        final JdbcParameterBinding binding;
        if (parameter == offsetParameter) {
            binding = new JdbcParameterBindingImpl(getIntegerType(), limit.getFirstRow());
        } else if (parameter == limitParameter) {
            binding = new JdbcParameterBindingImpl(getIntegerType(), limit.getMaxRows());
        } else {
            binding = jdbcParameterBindings.getBinding(parameter);
        }
        addAppliedParameterBinding(parameter, binding);
        return binding.getBindValue();
    }

    private BasicType<Integer> getIntegerType() {
        if (integerType == null) {
            integerType =
                    sessionFactory.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.INTEGER);
        }
        return integerType;
    }

    private BasicType<Boolean> getBooleanType() {
        if (booleanType == null) {
            booleanType =
                    sessionFactory.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN);
        }
        return booleanType;
    }

    private boolean hasLimit() {
        return limit != null && !limit.isEmpty();
    }

    private boolean needsRowsToSkip() {
        return false;
    }

    private boolean needsMaxRows() {
        return false;
    }

    private JdbcOperationQueryInsert translateInsert(InsertSelectStatement insertSelectStatement) {
        var root = (AstNode) renderInsertStatement(insertSelectStatement);
        return new JdbcOperationQueryInsertImpl(getMql(root), parameterBinders, affectedTableNames, null);
    }

    private JdbcOperation translateUpdate(UpdateStatement statement) {
        var root = (AstNode) renderUpdateStatementOnly(statement);

        return new JdbcOperationQueryUpdate(
                getMql(root), parameterBinders, affectedTableNames, appliedParameterBindings);

    }

    private AstNode renderUpdateStatementOnly(UpdateStatement statement) {
        if (statement.getFromClause().getRoots().size() > 1) {
            throw new NotSupportedRuntimeException("update statement with multiple roots not supported");
        }
        if (statement.getFromClause().getRoots().get(0).hasRealJoins()) {
            throw new NotSupportedRuntimeException("update statement with root having real joins not supported");
        }
        AstFilter filter = renderWhereClause(statement.getRestriction());
        List<AstFieldUpdate> updates = renderSetClause(statement.getAssignments());
        return new UpdateCommand(statement.getTargetTable().getTableExpression(), filter, updates);
    }

    private List<AstFieldUpdate> renderSetClause(final List<Assignment> assignments) {
        List<AstFieldUpdate> updates = new ArrayList<>(assignments.size());
        for (Assignment assignment : assignments) {
            updates.add(renderSetAssignment(assignment));
        }
        return updates;
    }

    private AstFieldUpdate renderSetAssignment(final Assignment assignment) {
        final List<ColumnReference> columnReferences =
                assignment.getAssignable().getColumnReferences();
        if (columnReferences.size() == 1) {
            ColumnReference columnReference = columnReferences.get(0);
            if (columnReference.getQualifier() != null) {
                // TODO: anything to do here?
            }
            final Expression assignedValue = assignment.getAssignedValue();
            final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(assignedValue);
            if (sqlTuple != null) {
                throw new NotSupportedRuntimeException();
            }
            AstValue value = (AstValue) accept(assignedValue);
            return new AstFieldUpdate(columnReference.getColumnExpression(), value);
        } else {
            throw new NotSupportedRuntimeException();
        }
    }

    private JdbcOperation translateDelete(DeleteStatement deleteStatement) {
        var root = (AstNode) renderDeleteStatement(deleteStatement);
        return new JdbcOperationQueryDelete(getMql(root), parameterBinders, affectedTableNames, null);
    }

    private T translateTableMutation(TableMutation<?> mutation) {
        AstNode root = (AstNode) accept(mutation);
        //noinspection unchecked
        return (T) mutation.createMutationOperation(getMql(root), parameterBinders);
    }

    private void cleanup() {
        this.jdbcParameterBindings = null;
        this.limit = null;
        this.offsetParameter = null;
        this.limitParameter = null;
    }

    @Override
    public Object renderSelectStatement(SelectStatement statement) {
        if (!statement.getQueryPart().isRoot()) {
            throw new UnsupportedOperationException("subquery unsupported");
        }
        return accept(statement.getQueryPart());
    }

    @Override
    public Object renderDeleteStatement(DeleteStatement statement) {
        return null;
    }

    @Override
    public Object renderUpdateStatement(UpdateStatement statement) {
        return null;
    }

    @Override
    public Object renderInsertStatement(InsertSelectStatement insertStatement) {
        return null;
    }

    @Override
    public Object renderAssignment(Assignment assignment) {
        return null;
    }

    @Override
    public Object renderQueryGroup(QueryGroup queryGroup) {
        return null;
    }

    @Override
    public Object renderQuerySpec(QuerySpec querySpec) {
        CollectionNameAndJoinStages collectionNameAndJoinStages =
                (CollectionNameAndJoinStages) renderFromClause(querySpec.getFromClause());
        List<AstStage> stageList = new ArrayList<>(collectionNameAndJoinStages.joinStages());
        AstFilter filter = renderWhereClause(querySpec.getWhereClauseRestrictions());
        stageList.add(new AstMatchStage(filter));

        if (CollectionUtil.isNotEmpty(querySpec.getSortSpecifications())) {
            stageList.add(new AstSortStage(renderOrderBy(querySpec.getSortSpecifications())));
        }

        List<AstProjectStageSpecification> projectStageSpecifications = renderSelectClause(querySpec.getSelectClause());
        stageList.add(new AstProjectStage(projectStageSpecifications));

        AstPipeline pipeline = new AstPipeline(stageList);
        return new AstAggregationCommand(collectionNameAndJoinStages.collectionName(), pipeline);
    }

    private AstFilter renderWhereClause(Predicate whereClauseRestrictions) {
        final boolean existsWhereClauseRestrictions =
                whereClauseRestrictions != null && !whereClauseRestrictions.isEmpty();
        if (existsWhereClauseRestrictions) {
            return (AstFilter) accept(whereClauseRestrictions);
        } else {
            return new AstMatchesEverythingFilter();
        }
    }

    private List<AstSortField> renderOrderBy(List<SortSpecification> sortSpecifications) {
        if (sortSpecifications != null && !sortSpecifications.isEmpty()) {
            List<AstSortField> sortFields = new ArrayList<>(sortSpecifications.size());
            for (SortSpecification sortSpecification : sortSpecifications) {
                sortFields.addAll(renderSortSpecification(sortSpecification));
            }
            return sortFields;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<AstSortField> renderSortSpecification(SortSpecification sortSpecification) {
        var sortExpression = sortSpecification.getSortExpression();
        var sortOrder = sortSpecification.getSortOrder();
        var sqlTuple = SqlTupleContainer.getSqlTuple(sortExpression);
        if (sqlTuple != null) {
            List<AstSortField> sortFields =
                    new ArrayList<>(sqlTuple.getExpressions().size());
            for (Expression expression : sqlTuple.getExpressions()) {
                sortFields.add(renderSortSpecification(expression, sortOrder));
            }
            return sortFields;
        } else {
            return Collections.singletonList(renderSortSpecification(sortExpression, sortOrder));
        }
    }

    private AstSortField renderSortSpecification(Expression sortExpression, SortDirection sortOrder) {
        var fieldName = (String) accept(sortExpression);
        AstSortOrder astSortOrder;
        if (sortOrder == SortDirection.DESCENDING) {
            astSortOrder = new AstDescendingSortOrder();
        } else if (sortOrder == SortDirection.ASCENDING) {
            astSortOrder = new AstAscendingSortOrder();
        } else {
            throw new NotYetImplementedException("Unclear if there are any other sort orders");
        }
        return new AstSortField(fieldName, astSortOrder);
    }

    @Override
    public Object renderOffsetFetchClause(QueryPart querySpec) {
        return null;
    }

    @Override
    public List<AstProjectStageSpecification> renderSelectClause(SelectClause selectClause) {
        final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
        final int size = sqlSelections.size();
        List<AstProjectStageSpecification> projectStageSpecifications = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            final SqlSelection sqlSelection = sqlSelections.get(i);
            if (sqlSelection.isVirtual()) {
                continue;
            }
            if (sqlSelection.getExpression() instanceof ColumnReference columnReference) {
                String columnReferenceAsString = pathTracker.renderColumnReference(columnReference);
                // TODO: checking for $ is a hack, but it will work
                AstExpression projectionExpression = columnReferenceAsString.startsWith("$")
                        ? new AstFieldPathExpression(columnReferenceAsString)
                        : new AstFormulaExpression(BsonDocument.parse(columnReferenceAsString));
                projectStageSpecifications.add(AstProjectStageSpecification.Set("f" + i, projectionExpression));
            } else {
                visitSqlSelection(sqlSelection);
            }
        }

        projectStageSpecifications.add(AstProjectStageSpecification.ExcludeId());
        return projectStageSpecifications;
    }

    @Override
    public Object renderSqlSelection(SqlSelection sqlSelection) {
        return null;
    }

    @Override
    public Object renderFromClause(FromClause fromClause) {
        if (fromClause == null || fromClause.getRoots().isEmpty()) {
            throw new NotSupportedRuntimeException("null fromClause or empty root not supported");
        } else {
            return renderFromClauseSpaces(fromClause);
        }
    }

    private Object renderFromClauseSpaces(final FromClause fromClause) {
        if (fromClause.getRoots().size() > 1) {
            throw new NotYetImplementedException();
        }
        return renderFromClauseRoot(fromClause.getRoots().get(0));
    }

    private Object renderFromClauseRoot(final TableGroup root) {
        String collectionName = (String) accept(root.getPrimaryTableReference());
        List<AstStage> joinStages = processTableGroupJoins(root);
        return new CollectionNameAndJoinStages(collectionName, joinStages);
    }

    private List<AstStage> processTableGroupJoins(final TableGroup source) {
        if (!source.getTableGroupJoins().isEmpty()) {
            List<AstStage> stages = new ArrayList<>(source.getTableGroupJoins().size());
            for (TableGroupJoin tableGroupJoin : source.getTableGroupJoins()) {
                stages.addAll(renderTableGroupJoin(source, tableGroupJoin));
            }
            return stages;
        } else {
            return Collections.emptyList();
        }
    }

    private List<AstStage> renderTableGroupJoin(final TableGroup source, final TableGroupJoin tableGroupJoin) {

        Predicate predicate = null;
        if (tableGroupJoin.getPredicate() == null) {
            if (tableGroupJoin.getJoinType() != SqlAstJoinType.CROSS) {
                predicate = new BooleanExpressionPredicate(new QueryLiteral<>(true, getBooleanType()));
            }
        } else {
            predicate = tableGroupJoin.getPredicate();
        }
        return renderTableGroup(source, tableGroupJoin.getJoinedGroup(), predicate);
    }

    private List<AstStage> renderTableGroup(
            final TableGroup source, final TableGroup tableGroup, @Nullable final Predicate predicate) {

        AstLookupStage lookupStage = simulateTableJoining(source, tableGroup, predicate);

        if (!tableGroup.getTableGroupJoins().isEmpty()) {
            List<AstStage> joinStages = processTableGroupJoins(tableGroup);
            lookupStage = lookupStage.addPipeline(joinStages);
        }

        return List.of(
                lookupStage,
                new AstUnwindStage(tableGroup.getPrimaryTableReference().getIdentificationVariable()));
    }

    private AstLookupStage simulateTableJoining(
            final TableGroup sourceTableGroup, final TableGroup targetTableGroup, @Nullable final Predicate predicate) {
        if (targetTableGroup.getPrimaryTableReference() instanceof NamedTableReference namedTargetTableReference) {
            var sourceQualifier = sourceTableGroup.getPrimaryTableReference().getIdentificationVariable();

            if (predicate instanceof ComparisonPredicate comparisonPredicate) {
                var targetQualifier =
                        targetTableGroup.getPrimaryTableReference().getIdentificationVariable();

                pathTracker.trackPath(sourceQualifier, targetQualifier);

                ColumnReference sourceColumnReference = null, targetColumnReference = null;
                var leftHandColumnReference =
                        comparisonPredicate.getLeftHandExpression().getColumnReference();
                var rightHandColumnReference =
                        comparisonPredicate.getRightHandExpression().getColumnReference();
                if (leftHandColumnReference.getQualifier().equals(targetQualifier)
                        && rightHandColumnReference.getQualifier().equals(sourceQualifier)) {
                    targetColumnReference = leftHandColumnReference;
                    sourceColumnReference = rightHandColumnReference;
                } else if (leftHandColumnReference.getQualifier().equals(sourceQualifier)
                        && rightHandColumnReference.getQualifier().equals(targetQualifier)) {
                    sourceColumnReference = leftHandColumnReference;
                    targetColumnReference = rightHandColumnReference;
                }
                AstLookupStageMatch lookupStageMatch;
                if (sourceColumnReference != null && targetColumnReference != null) {
                    lookupStageMatch = new AstLookupStageEqualityMatch(
                            sourceColumnReference.getColumnExpression(), targetColumnReference.getColumnExpression());
                } else {
                    throw new NotYetImplementedException(
                            "This appears to be untested code, so haven't added MQL AST support yet");
                }
                return new AstLookupStage(
                        namedTargetTableReference.getTableExpression(), targetQualifier, lookupStageMatch, List.of());
            } else {
                throw new NotYetImplementedException("currently only comparison predicate supported for table joining");
            }
        } else {
            throw new NotYetImplementedException("currently only NamedTableReference supported for table joining");
        }
    }

    @Override
    public Object renderTableGroup(TableGroup tableGroup) {
        return null;
    }

    @Override
    public Object renderTableGroupJoin(TableGroupJoin tableGroupJoin) {
        return null;
    }

    @Override
    public String renderNamedTableReference(NamedTableReference tableReference) {
        return tableReference.getTableExpression();
    }

    @Override
    public Object renderValuesTableReference(ValuesTableReference tableReference) {
        return null;
    }

    @Override
    public Object renderQueryPartTableReference(QueryPartTableReference tableReference) {
        return null;
    }

    @Override
    public Object renderFunctionTableReference(FunctionTableReference tableReference) {
        return null;
    }

    @Override
    public Object renderTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
        return null;
    }

    @Override
    public String renderColumnReference(ColumnReference columnReference) {
        return columnReference.getColumnExpression();
    }

    @Override
    public Object renderNestedColumnReference(NestedColumnReference nestedColumnReference) {
        return null;
    }

    @Override
    public Object renderAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression) {
        return null;
    }

    @Override
    public Object renderExtractUnit(ExtractUnit extractUnit) {
        return null;
    }

    @Override
    public Object renderFormat(Format format) {
        return null;
    }

    @Override
    public Object renderDistinct(Distinct distinct) {
        return null;
    }

    @Override
    public Object renderOverflow(Overflow overflow) {
        return null;
    }

    @Override
    public Object renderStar(Star star) {
        return null;
    }

    @Override
    public Object renderTrimSpecification(TrimSpecification trimSpecification) {
        return null;
    }

    @Override
    public Object renderCastTarget(CastTarget castTarget) {
        return null;
    }

    @Override
    public Object renderBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
        return null;
    }

    @Override
    public Object renderCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
        return null;
    }

    @Override
    public Object renderCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
        return null;
    }

    @Override
    public Object renderAny(Any any) {
        return null;
    }

    @Override
    public Object renderEvery(Every every) {
        return null;
    }

    @Override
    public Object renderSummarization(Summarization every) {
        return null;
    }

    @Override
    public <E> AstNode renderOver(Over<E> over) {
        return null;
    }

    @Override
    public Object renderSelfRenderingExpression(SelfRenderingExpression expression) {
        return null;
    }

    @Override
    public Object renderSqlSelectionExpression(SqlSelectionExpression expression) {
        return null;
    }

    @Override
    public Object renderEntityTypeLiteral(EntityTypeLiteral expression) {
        return null;
    }

    @Override
    public Object renderEmbeddableTypeLiteral(EmbeddableTypeLiteral expression) {
        return null;
    }

    @Override
    public Object renderTuple(SqlTuple tuple) {
        return null;
    }

    @Override
    public Object renderCollation(Collation collation) {
        return null;
    }

    @Override
    public AstPlaceholder renderParameter(JdbcParameter jdbcParameter) {
        return renderParameterAsParameter(jdbcParameter);
    }


    private AstPlaceholder renderParameterAsParameter(JdbcParameter jdbcParameter) {
        parameterBinders.add(jdbcParameter.getParameterBinder());
        jdbcParameters.addParameter(jdbcParameter);
        return new AstPlaceholder();
    }

    @Override
    public <E> AstNode renderJdbcLiteral(JdbcLiteral<E> jdbcLiteral) {
        return null;
    }

    @Override
    public <E> AstNode renderQueryLiteral(QueryLiteral<E> queryLiteral) {
        return null;
    }

    @Override
    public <N extends Number> AstNode renderUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal) {
        return null;
    }

    @Override
    public Object renderUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
        return null;
    }

    @Override
    public Object renderModifiedSubQueryExpression(ModifiedSubQueryExpression expression) {
        return null;
    }

    @Override
    public Object renderBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
        return null;
    }

    @Override
    public Object renderBetweenPredicate(BetweenPredicate betweenPredicate) {
        return null;
    }

    @Override
    public Object renderFilterPredicate(FilterPredicate filterPredicate) {
        return null;
    }

    @Override
    public Object renderFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate) {
        return null;
    }

    @Override
    public Object renderSqlFragmentPredicate(SqlFragmentPredicate predicate) {
        return null;
    }

    @Override
    public Object renderGroupedPredicate(GroupedPredicate groupedPredicate) {
        return null;
    }

    @Override
    public Object renderInListPredicate(InListPredicate inListPredicate) {
        return null;
    }

    @Override
    public Object renderInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
        return null;
    }

    @Override
    public Object renderInArrayPredicate(InArrayPredicate inArrayPredicate) {
        return null;
    }

    @Override
    public Object renderExistsPredicate(ExistsPredicate existsPredicate) {
        return null;
    }

    @Override
    public Object renderJunction(Junction junction) {
        return null;
    }

    @Override
    public Object renderLikePredicate(LikePredicate likePredicate) {
        return null;
    }

    @Override
    public Object renderNegatedPredicate(NegatedPredicate negatedPredicate) {
        return null;
    }

    @Override
    public Object renderNullnessPredicate(NullnessPredicate nullnessPredicate) {
        return null;
    }

    @Override
    public Object renderThruthnessPredicate(ThruthnessPredicate predicate) {
        return null;
    }

    @Override
    public Object renderRelationalPredicate(ComparisonPredicate comparisonPredicate) {
        return renderComparisonStandard(
                comparisonPredicate.getLeftHandExpression(),
                comparisonPredicate.getOperator(),
                comparisonPredicate.getRightHandExpression());
    }

    private AstFieldOperationFilter renderComparisonStandard(
            final Expression lhs, final ComparisonOperator operator, final Expression rhs) {
        String fieldName = (String) accept(lhs);

        AstValue value = (AstValue) accept(rhs);
        return new AstFieldOperationFilter(
                new AstFilterField(fieldName),
                new AstComparisonFilterOperation(convertOperator(operator), value));
    }

    @Override
    public Object renderSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
        return null;
    }

    @Override
    public Object renderDurationUnit(DurationUnit durationUnit) {
        return null;
    }

    @Override
    public Object renderDuration(Duration duration) {
        return null;
    }

    @Override
    public Object renderConversion(Conversion conversion) {
        return null;
    }

    @Override
    public Object renderStandardTableInsert(TableInsertStandard tableInsert) {
        String collectionName = tableInsert.getMutatingTable().getTableName();

        List<AstElement> elements = new ArrayList<>(tableInsert.getNumberOfValueBindings());
        tableInsert.forEachValueBinding((columnPosition, columnValueBinding) -> {
            AstValue value = (AstValue) accept(columnValueBinding.getValueExpression());
            elements.add(new AstElement(columnValueBinding.getColumnReference().getColumnExpression(), value));
        });

        return new AstInsertCommand(collectionName, elements);
    }

    @Override
    public Object renderCustomTableInsert(TableInsertCustomSql tableInsert) {
        return null;
    }

    @Override
    public Object renderStandardTableDelete(TableDeleteStandard tableDelete) {
        String collectionName = tableDelete.getMutatingTable().getTableName();
        List<AstFilter> filters = new ArrayList<>(tableDelete.getNumberOfKeyBindings());
        tableDelete.forEachKeyBinding((columnPosition, columnValueBinding) -> {
            AstValue value = (AstValue) accept(columnValueBinding.getValueExpression());
            filters.add(new AstFieldOperationFilter(
                    new AstFilterField(columnValueBinding.getColumnReference().getColumnExpression()),
                    new AstComparisonFilterOperation(AstComparisonFilterOperator.EQ, value)));
        });

        return new DeleteCommand(collectionName, new AstAndFilter(filters));
    }

    @Override
    public Object renderCustomTableDelete(TableDeleteCustomSql tableDelete) {
        return null;
    }

    @Override
    public Object renderStandardTableUpdate(TableUpdateStandard tableUpdate) {
        registerAffectedTable(tableUpdate.getMutatingTable().getTableName());
        List<AstFilter> filters = new ArrayList<>(tableUpdate.getNumberOfKeyBindings());
        tableUpdate.forEachKeyBinding((position, columnValueBinding) -> {
            AstValue value = (AstValue) accept(columnValueBinding.getValueExpression());
            filters.add(new AstFieldOperationFilter(
                    new AstFilterField(columnValueBinding.getColumnReference().getColumnExpression()),
                    new AstComparisonFilterOperation(AstComparisonFilterOperator.EQ, value)));
        });
        List<AstFieldUpdate> updates = new ArrayList<>(tableUpdate.getNumberOfValueBindings());
        tableUpdate.forEachValueBinding((columnPosition, columnValueBinding) -> {
            AstValue value = (AstValue) accept(columnValueBinding.getValueExpression());
            updates.add(
                    new AstFieldUpdate(columnValueBinding.getColumnReference().getColumnExpression(), value));
        });
        return new UpdateCommand(tableUpdate.getMutatingTable().getTableName(), new AstAndFilter(filters), updates);
    }

    @Override
    public Object renderOptionalTableUpdate(OptionalTableUpdate tableUpdate) {
        return null;
    }

    @Override
    public Object renderCustomTableUpdate(TableUpdateCustomSql tableUpdate) {
        return null;
    }

    @Override
    public Object renderColumnWriteFragment(ColumnWriteFragment columnWriteFragment) {
        if (CollectionHelper.isEmpty(columnWriteFragment.getParameters())
                || ParameterMarkerStrategyStandard.isStandardRenderer(parameterMarkerStrategy)) {
            throw new UnsupportedOperationException("no parameter unsupported");
        }
        if (columnWriteFragment.getParameters().size() > 1) {
            throw new UnsupportedOperationException("multiple parameters unsupported");
        }
        var parameter = columnWriteFragment.getParameters().iterator().next();
        parameterBinders.add(parameter.getParameterBinder());
        jdbcParameters.addParameter(parameter);
        return new AstPlaceholder();
    }

    private static String getMql(AstNode root) {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        root.render(jsonWriter);
        return writer.toString();
    }

    AstComparisonFilterOperator convertOperator(final ComparisonOperator operator) {
        switch (operator) {
            case EQUAL -> {
                return AstComparisonFilterOperator.EQ;
            }
            case NOT_EQUAL -> {
                return AstComparisonFilterOperator.NE;
            }
            case LESS_THAN -> {
                return AstComparisonFilterOperator.LT;
            }
            case LESS_THAN_OR_EQUAL -> {
                return AstComparisonFilterOperator.LTE;
            }
            case GREATER_THAN -> {
                return AstComparisonFilterOperator.GT;
            }
            case GREATER_THAN_OR_EQUAL -> {
                return AstComparisonFilterOperator.GTE;
            }
            default -> {
                throw new NotSupportedRuntimeException();
            }
        }
    }
}
