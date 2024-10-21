package org.hibernate.omm.translate.translator;

import com.mongodb.lang.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.BsonDocument;
import org.hibernate.dialect.SelectItemReferenceStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.omm.exception.NotSupportedRuntimeException;
import org.hibernate.omm.exception.NotYetImplementedException;
import org.hibernate.omm.translate.translator.mongoast.AstAggregationCommand;
import org.hibernate.omm.translate.translator.mongoast.AstAscendingSortOrder;
import org.hibernate.omm.translate.translator.mongoast.AstDescendingSortOrder;
import org.hibernate.omm.translate.translator.mongoast.AstPipeline;
import org.hibernate.omm.translate.translator.mongoast.AstSortField;
import org.hibernate.omm.translate.translator.mongoast.AstSortOrder;
import org.hibernate.omm.translate.translator.mongoast.AstValue;
import org.hibernate.omm.translate.translator.mongoast.expressions.AstExpression;
import org.hibernate.omm.translate.translator.mongoast.expressions.AstFieldPathExpression;
import org.hibernate.omm.translate.translator.mongoast.expressions.AstFormulaExpression;
import org.hibernate.omm.translate.translator.mongoast.filters.AstComparisonFilterOperation;
import org.hibernate.omm.translate.translator.mongoast.filters.AstComparisonFilterOperator;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFieldOperationFilter;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFilter;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFilterField;
import org.hibernate.omm.translate.translator.mongoast.stages.AstLookupStage;
import org.hibernate.omm.translate.translator.mongoast.stages.AstLookupStageEqualityMatch;
import org.hibernate.omm.translate.translator.mongoast.stages.AstLookupStageMatch;
import org.hibernate.omm.translate.translator.mongoast.stages.AstMatchStage;
import org.hibernate.omm.translate.translator.mongoast.stages.AstProjectStage;
import org.hibernate.omm.translate.translator.mongoast.stages.AstProjectStageSpecification;
import org.hibernate.omm.translate.translator.mongoast.stages.AstSortStage;
import org.hibernate.omm.translate.translator.mongoast.stages.AstStage;
import org.hibernate.omm.translate.translator.mongoast.stages.AstUnwindStage;
import org.hibernate.omm.util.CollectionUtil;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortDirection;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;

/**
 * Contains MQL rendering logic.
 *
 * @see BsonCommandTranslator
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MQLTranslator extends AbstractBsonTranslator<JdbcOperationQuerySelect> {

    private static class PathTracker {
        private final Map<String, String> pathByQualifier = new HashMap<>();

        @Nullable private String rootQualifier;

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

    @Nullable private String targetQualifier;

    public MQLTranslator(final SessionFactoryImplementor sessionFactory, final Statement statement) {
        super(sessionFactory, statement);
    }

    @Override
    public void visitQuerySpec(final QuerySpec querySpec) {
        final QueryPart queryPartForRowNumbering = this.queryPartForRowNumbering;
        final int queryPartForRowNumberingClauseDepth = this.queryPartForRowNumberingClauseDepth;
        final boolean needsSelectAliases = this.needsSelectAliases;
        final Predicate additionalWherePredicate = this.additionalWherePredicate;
        final ForUpdateClause forUpdate = this.forUpdate;
        try {
            this.additionalWherePredicate = null;
            this.forUpdate = null;
            // See the field documentation of queryPartForRowNumbering etc. for an explanation about this
            // In addition, we also reset the row numbering if the currently row numbered query part is a
            // query group
            // which means this query spec is a part of that query group.
            // We want the row numbering to happen on the query group level, not on the query spec level,
            // so we reset
            final QueryPart currentQueryPart = queryPartStack.getCurrent();
            if (currentQueryPart != null
                    && (queryPartForRowNumbering instanceof QueryGroup
                            || queryPartForRowNumberingClauseDepth != clauseStack.depth())) {
                this.queryPartForRowNumbering = null;
                this.queryPartForRowNumberingClauseDepth = -1;
            }
            String queryGroupAlias = null;
            if (currentQueryPart instanceof QueryGroup) {
                // We always need query wrapping if we are in a query group and this query spec has a fetch
                // or order by
                // clause, because of order by precedence in SQL
                if (querySpec.hasOffsetOrFetchClause() || querySpec.hasSortSpecifications()) {
                    throw new NotSupportedRuntimeException();
                    //                    queryGroupAlias = "";
                    // If the parent is a query group with a fetch clause we must use a select wrapper,
                    // or if the database does not support simple query grouping, we must use a select wrapper
                    //                    if ((!supportsSimpleQueryGrouping() ||
                    // currentQueryPart.hasOffsetOrFetchClause())
                    // We can skip it though if this query spec is being row numbered,
                    // because then we already have a wrapper
                    //                            && queryPartForRowNumbering != querySpec) {
                    //                        throw new NotSupportedRuntimeException();
                    //                        // We need to assign aliases when we render a query spec as
                    // subquery to avoid clashing aliases
                    //                        this.needsSelectAliases = this.needsSelectAliases ||
                    // hasDuplicateSelectItems(querySpec);
                    //                    } else if (!supportsDuplicateSelectItemsInQueryGroup()) {
                    //                        this.needsSelectAliases = this.needsSelectAliases ||
                    // hasDuplicateSelectItems(querySpec);
                    //                    }
                }
            }
            queryPartStack.push(querySpec);
            //            if (queryGroupAlias != null) {
            //                throw new NotSupportedRuntimeException("query group not supported");
            //            }
            CollectionNameAndJoinStages collectionNameAndJoinStages = mqlAstState.expect(
                    AttachmentKeys.collectionNameAndJoinStages(), () -> visitFromClause(querySpec.getFromClause()));

            List<AstStage> stageList = new ArrayList<>(collectionNameAndJoinStages.joinStages());

            AstFilter filter = mqlAstState.expect(
                    AttachmentKeys.filter(), () -> visitWhereClause(querySpec.getWhereClauseRestrictions()));
            stageList.add(new AstMatchStage(filter));

            if (CollectionUtil.isNotEmpty(querySpec.getSortSpecifications())) {
                List<AstSortField> sortFields = mqlAstState.expect(
                        AttachmentKeys.sortFields(), () -> visitOrderBy(querySpec.getSortSpecifications()));
                stageList.add(new AstSortStage(sortFields));
            }

            List<AstProjectStageSpecification> projectStageSpecifications = mqlAstState.expect(
                    AttachmentKeys.projectStageSpecifications(), () -> visitSelectClause(querySpec.getSelectClause()));
            stageList.add(new AstProjectStage(projectStageSpecifications));

            // visitGroupByClause( querySpec, dialect.getGroupBySelectItemReferenceStrategy() );
            // visitHavingClause( querySpec );
            visitOffsetFetchClause(querySpec);
            // We render the FOR UPDATE clause in the parent query
            // if ( queryPartForRowNumbering == null ) {
            // visitForUpdateClause( querySpec );
            // }
            AstPipeline pipeline = new AstPipeline(stageList);
            root = new AstAggregationCommand(collectionNameAndJoinStages.collectionName(), pipeline);
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

    @Override
    public void visitFromClause(@Nullable final FromClause fromClause) {
        if (fromClause == null || fromClause.getRoots().isEmpty()) {
            throw new NotSupportedRuntimeException("null fromClause or empty root not supported");
        } else {
            renderFromClauseSpaces(fromClause);
        }
    }

    @Override
    protected void renderFromClauseSpaces(final FromClause fromClause) {
        try {
            clauseStack.push(Clause.FROM);
            if (fromClause.getRoots().size() > 1) {
                throw new NotYetImplementedException();
            }
            pathTracker.setRootQualifier(
                    fromClause.getRoots().get(0).getPrimaryTableReference().getIdentificationVariable());
            renderFromClauseRoot(fromClause.getRoots().get(0));
        } finally {
            clauseStack.pop();
        }
    }

    private void renderFromClauseRoot(final TableGroup root) {
        if (root.isVirtual()) {
            throw new NotYetImplementedException();
        } else if (root.isInitialized()) {
            renderRootTableGroup(root, null);
        }
    }

    @Override
    protected void renderRootTableGroup(
            final TableGroup tableGroup, @Nullable final List<TableGroupJoin> tableGroupJoinCollector) {

        renderTableReferenceJoins(tableGroup);
        processNestedTableGroupJoins(tableGroup, tableGroupJoinCollector);
        if (tableGroupJoinCollector != null) {
            tableGroupJoinCollector.addAll(tableGroup.getTableGroupJoins());
        } else {
            String collectionName = mqlAstState.expect(
                    AttachmentKeys.collectionName(),
                    () -> tableGroup.getPrimaryTableReference().accept(this));
            List<AstStage> joinStages =
                    mqlAstState.expect(AttachmentKeys.joinStages(), () -> processTableGroupJoins(tableGroup));
            mqlAstState.attach(
                    AttachmentKeys.collectionNameAndJoinStages(),
                    new CollectionNameAndJoinStages(collectionName, joinStages));
        }
        ModelPartContainer modelPart = tableGroup.getModelPart();
        if (modelPart instanceof AbstractEntityPersister) {
            String[] querySpaces = (String[]) ((AbstractEntityPersister) modelPart).getQuerySpaces();
            for (String querySpace : querySpaces) {
                registerAffectedTable(querySpace);
            }
        }
    }

    @Override
    protected void processTableGroupJoins(final TableGroup source) {
        if (!source.getTableGroupJoins().isEmpty()) {
            for (int i = 0; i < source.getTableGroupJoins().size(); i++) {
                processTableGroupJoin(source, source.getTableGroupJoins().get(i), null);
            }
        } else {
            mqlAstState.attach(AttachmentKeys.joinStages(), List.of());
        }
    }

    @Override
    public void visitSelectClause(final SelectClause selectClause) {
        clauseStack.push(Clause.SELECT);
        try {
            /*if ( selectClause.isDistinct() ) {
                appendMql( "distinct " );
            }*/
            visitSqlSelections(selectClause);
        } finally {
            clauseStack.pop();
        }
    }

    @Override
    protected void visitSqlSelections(final SelectClause selectClause) {
        final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
        final int size = sqlSelections.size();
        List<AstProjectStageSpecification> projectStageSpecifications = new ArrayList<>(size);
        final SelectItemReferenceStrategy referenceStrategy = dialect.getGroupBySelectItemReferenceStrategy();
        // When the dialect needs to render the aliased expression and there are aliased group by items,
        // we need to inline parameters as the database would otherwise not be able to match the group
        // by item
        // to the select item, ultimately leading to a query error
        final BitSet selectItemsToInline;
        if (referenceStrategy == SelectItemReferenceStrategy.EXPRESSION) {
            selectItemsToInline = getSelectItemsToInline();
        } else {
            selectItemsToInline = null;
        }
        final SqlAstNodeRenderingMode original = parameterRenderingMode;
        final SqlAstNodeRenderingMode defaultRenderingMode;
        if (getStatement() instanceof InsertSelectStatement
                && clauseStack.depth() == 1
                && queryPartStack.depth() == 1) {
            // Databases support inferring parameter types for simple insert-select statements
            defaultRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
        } else {
            defaultRenderingMode = SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER;
        }
        if (needsSelectAliases
                || referenceStrategy == SelectItemReferenceStrategy.ALIAS && hasSelectAliasInGroupByClause()) {
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
                        renderSelectExpression(e);
                    }
                } else {
                    renderSelectExpression(expression);
                }
                parameterRenderingMode = original;
            }
            if (queryPartForRowNumbering != null) {
                renderRowNumberingSelectItems(selectClause, queryPartForRowNumbering);
            }
        } else {
            assert columnAliases == null;
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
                parameterRenderingMode = original;
            }

            projectStageSpecifications.add(AstProjectStageSpecification.ExcludeId());
            mqlAstState.attach(AttachmentKeys.projectStageSpecifications(), projectStageSpecifications);
        }
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

    private void processTableGroupJoin(
            final TableGroup source,
            final TableGroupJoin tableGroupJoin,
            @Nullable final List<TableGroupJoin> tableGroupJoinCollector) {
        final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();

        if (joinedGroup.isVirtual()) {
            processNestedTableGroupJoins(joinedGroup, tableGroupJoinCollector);
            if (tableGroupJoinCollector != null) {
                tableGroupJoinCollector.addAll(joinedGroup.getTableGroupJoins());
            } else {
                processTableGroupJoins(joinedGroup);
            }
        } else if (joinedGroup.isInitialized()) {
            renderTableGroupJoin(source, tableGroupJoin, tableGroupJoinCollector);
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

    private void renderTableGroupJoin(
            final TableGroup source,
            final TableGroupJoin tableGroupJoin,
            @Nullable final List<TableGroupJoin> tableGroupJoinCollector) {
        // appendMql(tableGroupJoin.getJoinType().getText());

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
            renderTableGroup(source, tableGroupJoin.getJoinedGroup(), predicate, tableGroupJoinCollector);
        } else {
            renderTableGroup(source, tableGroupJoin.getJoinedGroup(), null, tableGroupJoinCollector);
        }
    }

    private void renderTableGroup(
            final TableGroup source,
            final TableGroup tableGroup,
            @Nullable final Predicate predicate,
            @Nullable final List<TableGroupJoin> tableGroupJoinCollector) {

        // Without reference joins or nested join groups, even a real table group does not need
        // parenthesis
        final boolean realTableGroup = tableGroup.isRealTableGroup()
                && (CollectionHelper.isNotEmpty(tableGroup.getTableReferenceJoins())
                        || hasNestedTableGroupsToRender(tableGroup.getNestedTableGroupJoins()));
        if (realTableGroup) {
            // appendMql(OPEN_PARENTHESIS);
        }

        // final boolean usesLockHint = renderPrimaryTableReference(tableGroup, effectiveLockMode);
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
            // appendMql(CLOSE_PARENTHESIS);
        } else {
            tableGroupJoins = null;
        }

        AstLookupStage lookupStage = mqlAstState.expect(
                AttachmentKeys.lookupStage(), () -> simulateTableJoining(source, tableGroup, predicate));

        if (tableGroup.isLateral() && !dialect.supportsLateral()) {
            final Predicate lateralEmulationPredicate = determineLateralEmulationPredicate(tableGroup);
            if (lateralEmulationPredicate != null) {
                throw new NotSupportedRuntimeException();
                // TODO: untested
                //                if (predicate == null) {
                //                    appendMql(" on ");
                //                } else {
                //                    appendMql(" and ");
                //                }
                //                lateralEmulationPredicate.accept(this);
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
                    processTableGroupJoin(source, tableGroupJoin, null);
                }
            }
            if (!tableGroup.getTableGroupJoins().isEmpty()) {
                List<AstStage> joinStages =
                        mqlAstState.expect(AttachmentKeys.joinStages(), () -> processTableGroupJoins(tableGroup));
                lookupStage = lookupStage.addPipeline(joinStages);
            }
        }

        mqlAstState.attach(
                AttachmentKeys.joinStages(),
                List.of(
                        lookupStage,
                        new AstUnwindStage(tableGroup.getPrimaryTableReference().getIdentificationVariable())));
        ModelPartContainer modelPart = tableGroup.getModelPart();
        if (modelPart instanceof AbstractEntityPersister) {
            String[] querySpaces = (String[]) ((AbstractEntityPersister) modelPart).getQuerySpaces();
            for (String querySpace : querySpaces) {
                registerAffectedTable(querySpace);
            }
        }
    }

    private void simulateTableJoining(
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
                    var sourceColumnsInPredicate = getSourceColumnsInPredicate(comparisonPredicate, sourceQualifier);
                    //                    if (!sourceColumnsInPredicate.isEmpty()) {
                    //                        // TODO: untested
                    //                    }
                    try {
                        this.targetQualifier = sourceQualifier;
                        setInAggregateExpressionScope(true);
                        predicate.accept(this);
                    } finally {
                        setInAggregateExpressionScope(false);
                        this.targetQualifier = null;
                    }
                    throw new NotYetImplementedException(
                            "This appears to be untested code, so haven't added MQL AST support yet");
                }
                mqlAstState.attach(
                        AttachmentKeys.lookupStage(),
                        new AstLookupStage(
                                namedTargetTableReference.getTableExpression(),
                                targetQualifier,
                                lookupStageMatch,
                                List.of()));
            } else {
                throw new NotYetImplementedException("currently only comparison predicate supported for table joining");
            }
        } else {
            throw new NotYetImplementedException("currently only NamedTableReference supported for table joining");
        }
    }

    private List<String> getSourceColumnsInPredicate(
            final ComparisonPredicate comparisonPredicate, final String sourceAlias) {
        List<String> sourceColumns = new ArrayList<>();
        List<ColumnReference> columnReferences = new ArrayList<>();
        if (comparisonPredicate.getLeftHandExpression() instanceof ColumnReference columnReference) {
            columnReferences.add(columnReference);
        }
        if (comparisonPredicate.getRightHandExpression() instanceof ColumnReference columnReference) {
            columnReferences.add(columnReference);
        }

        for (ColumnReference columnReference : columnReferences) {
            if (columnReference.getQualifier().equals(sourceAlias) && !columnReference.isColumnExpressionFormula()) {
                sourceColumns.add(columnReference.getColumnExpression());
            }
        }
        return sourceColumns;
    }

    @Override
    public void visitColumnReference(final ColumnReference columnReference) {
        if (targetQualifier != null && !columnReference.isColumnExpressionFormula()) {
            throw new NotSupportedRuntimeException();
        } else if (queryPartStack.getCurrent() instanceof QuerySpec) {
            if (!columnReference.getQualifier().equals(pathTracker.getRootQualifier())) {
                throw new NotSupportedRuntimeException();
            } else {
                mqlAstState.attach(AttachmentKeys.fieldName(), columnReference.getColumnExpression());
            }
        } else {
            columnReference.appendReadExpression(this, null);
        }
    }

    @Override
    protected void renderComparisonStandard(
            final Expression lhs, final ComparisonOperator operator, final Expression rhs) {
        if (isInAggregateExpressionScope()) {
            throw new NotSupportedRuntimeException();
        } else {
            String fieldName = mqlAstState.expect(AttachmentKeys.fieldName(), () -> lhs.accept(this));
            AstValue value = mqlAstState.expect(AttachmentKeys.fieldValue(), () -> rhs.accept(this));
            mqlAstState.attach(
                    AttachmentKeys.filter(),
                    new AstFieldOperationFilter(
                            new AstFilterField(fieldName),
                            new AstComparisonFilterOperation(getComparisonFilterOperator(operator), value)));
        }
    }

    @Override
    protected void renderOrderBy(final boolean addWhitespace, final List<SortSpecification> sortSpecifications) {
        List<AstSortField> sortFields = new ArrayList<>();
        if (sortSpecifications != null && !sortSpecifications.isEmpty()) {
            clauseStack.push(Clause.ORDER);
            try {
                for (SortSpecification sortSpecification : sortSpecifications) {
                    sortFields.add(mqlAstState.expect(
                            AttachmentKeys.sortField(), () -> visitSortSpecification(sortSpecification)));
                }
            } finally {
                clauseStack.pop();
            }
        }
        mqlAstState.attach(AttachmentKeys.sortFields(), sortFields);
    }

    @Override
    protected void visitSortSpecification(
            final Expression sortExpression,
            final SortDirection sortOrder,
            final NullPrecedence nullPrecedence,
            final boolean ignoreCase) {
        if (nullPrecedence == NullPrecedence.LAST) {
            throw new NotSupportedRuntimeException("Mongo only supports 'null goes first'");
        }

        String fieldName =
                mqlAstState.expect(AttachmentKeys.fieldName(), () -> renderSortExpression(sortExpression, ignoreCase));

        AstSortOrder astSortOrder;
        if (sortOrder == SortDirection.DESCENDING) {
            astSortOrder = new AstDescendingSortOrder();
        } else if (sortOrder == SortDirection.ASCENDING) {
            astSortOrder = new AstAscendingSortOrder();
        } else {
            throw new NotYetImplementedException("Unclear if there are any other sort orders");
        }
        mqlAstState.attach(AttachmentKeys.sortField(), new AstSortField(fieldName, astSortOrder));
    }

    private AstComparisonFilterOperator getComparisonFilterOperator(final ComparisonOperator operator) {
        return switch (operator) {
            case EQUAL -> AstComparisonFilterOperator.EQ;
            case NOT_EQUAL -> AstComparisonFilterOperator.NE;
            case LESS_THAN -> AstComparisonFilterOperator.LT;
            case LESS_THAN_OR_EQUAL -> AstComparisonFilterOperator.LTE;
            case GREATER_THAN -> AstComparisonFilterOperator.GT;
            case GREATER_THAN_OR_EQUAL -> AstComparisonFilterOperator.GTE;
            default -> throw new NotSupportedRuntimeException("unsupported operator: " + operator.name());
        };
    }
}
