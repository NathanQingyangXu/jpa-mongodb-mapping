package org.hibernate.omm.translate;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.omm.translate.translator.mongoast.AstNode;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
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
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.internal.*;

import java.util.HashSet;
import java.util.Set;

public class AbstractMongoSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstWalker
        implements MqlAstWalker, SqlAstTranslator<T> {

    private final SessionFactoryImplementor sessionFactory;

    private final Stack<Clause> clauseStack = new StandardStack<>(Clause.class);
    private final Stack<QueryPart> queryPartStack = new StandardStack<>(QueryPart.class);
    private final Stack<Statement> statementStack = new StandardStack<>(Statement.class);

    private final Set<String> affectedTableNames = new HashSet<>();

    private JdbcParameterBindings jdbcParameterBindings;

    private Limit limit;
    private JdbcParameter offsetParameter;
    private JdbcParameter limitParameter;

    public AbstractMongoSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
        this.sessionFactory = sessionFactory;
        this.statementStack.push(statement);
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

    @Override
    public Set<String> getAffectedTableNames() {
        return affectedTableNames;
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

    private JdbcOperation translateSelect(SelectStatement statement) {
        return null; //TODO
    }

    private JdbcOperation translateInsert(InsertSelectStatement statement) {
        return null; //TODO
    }

    private JdbcOperation translateUpdate(UpdateStatement statement) {
        return null; //TODO
    }

    private JdbcOperation translateDelete(DeleteStatement statement) {
        return null; //TODO
    }

    private T translateTableMutation(TableMutation<?> statement) {
        return null; //TODO
    }

    protected void cleanup() {
        this.jdbcParameterBindings = null;
        this.limit = null;
        this.offsetParameter = null;
        this.limitParameter = null;
    }

    @Override
    public AstNode renderSelectStatement(SelectStatement statement) {
        return null;
    }

    @Override
    public AstNode renderDeleteStatement(DeleteStatement statement) {
        return null;
    }

    @Override
    public AstNode renderUpdateStatement(UpdateStatement statement) {
        return null;
    }

    @Override
    public AstNode renderInsertStatement(InsertSelectStatement statement) {
        return null;
    }

    @Override
    public AstNode renderAssignment(Assignment assignment) {
        return null;
    }

    @Override
    public AstNode renderQueryGroup(QueryGroup queryGroup) {
        return null;
    }

    @Override
    public AstNode renderQuerySpec(QuerySpec querySpec) {
        return null;
    }

    @Override
    public AstNode renderSortSpecification(SortSpecification sortSpecification) {
        return null;
    }

    @Override
    public AstNode renderOffsetFetchClause(QueryPart querySpec) {
        return null;
    }

    @Override
    public AstNode renderSelectClause(SelectClause selectClause) {
        return null;
    }

    @Override
    public AstNode renderSqlSelection(SqlSelection sqlSelection) {
        return null;
    }

    @Override
    public AstNode renderFromClause(FromClause fromClause) {
        return null;
    }

    @Override
    public AstNode renderTableGroup(TableGroup tableGroup) {
        return null;
    }

    @Override
    public AstNode renderTableGroupJoin(TableGroupJoin tableGroupJoin) {
        return null;
    }

    @Override
    public AstNode renderNamedTableReference(NamedTableReference tableReference) {
        return null;
    }

    @Override
    public AstNode renderValuesTableReference(ValuesTableReference tableReference) {
        return null;
    }

    @Override
    public AstNode renderQueryPartTableReference(QueryPartTableReference tableReference) {
        return null;
    }

    @Override
    public AstNode renderFunctionTableReference(FunctionTableReference tableReference) {
        return null;
    }

    @Override
    public AstNode renderTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
        return null;
    }

    @Override
    public AstNode renderColumnReference(ColumnReference columnReference) {
        return null;
    }

    @Override
    public AstNode renderNestedColumnReference(NestedColumnReference nestedColumnReference) {
        return null;
    }

    @Override
    public AstNode renderAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression) {
        return null;
    }

    @Override
    public AstNode renderExtractUnit(ExtractUnit extractUnit) {
        return null;
    }

    @Override
    public AstNode renderFormat(Format format) {
        return null;
    }

    @Override
    public AstNode renderDistinct(Distinct distinct) {
        return null;
    }

    @Override
    public AstNode renderOverflow(Overflow overflow) {
        return null;
    }

    @Override
    public AstNode renderStar(Star star) {
        return null;
    }

    @Override
    public AstNode renderTrimSpecification(TrimSpecification trimSpecification) {
        return null;
    }

    @Override
    public AstNode renderCastTarget(CastTarget castTarget) {
        return null;
    }

    @Override
    public AstNode renderBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
        return null;
    }

    @Override
    public AstNode renderCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
        return null;
    }

    @Override
    public AstNode renderCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
        return null;
    }

    @Override
    public AstNode renderAny(Any any) {
        return null;
    }

    @Override
    public AstNode renderEvery(Every every) {
        return null;
    }

    @Override
    public AstNode renderSummarization(Summarization every) {
        return null;
    }

    @Override
    public <E> AstNode renderOver(Over<E> over) {
        return null;
    }

    @Override
    public AstNode renderSelfRenderingExpression(SelfRenderingExpression expression) {
        return null;
    }

    @Override
    public AstNode renderSqlSelectionExpression(SqlSelectionExpression expression) {
        return null;
    }

    @Override
    public AstNode renderEntityTypeLiteral(EntityTypeLiteral expression) {
        return null;
    }

    @Override
    public AstNode renderEmbeddableTypeLiteral(EmbeddableTypeLiteral expression) {
        return null;
    }

    @Override
    public AstNode renderTuple(SqlTuple tuple) {
        return null;
    }

    @Override
    public AstNode renderCollation(Collation collation) {
        return null;
    }

    @Override
    public AstNode renderParameter(JdbcParameter jdbcParameter) {
        return null;
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
    public AstNode renderUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
        return null;
    }

    @Override
    public AstNode renderModifiedSubQueryExpression(ModifiedSubQueryExpression expression) {
        return null;
    }

    @Override
    public AstNode renderBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
        return null;
    }

    @Override
    public AstNode renderBetweenPredicate(BetweenPredicate betweenPredicate) {
        return null;
    }

    @Override
    public AstNode renderFilterPredicate(FilterPredicate filterPredicate) {
        return null;
    }

    @Override
    public AstNode renderFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate) {
        return null;
    }

    @Override
    public AstNode renderSqlFragmentPredicate(SqlFragmentPredicate predicate) {
        return null;
    }

    @Override
    public AstNode renderGroupedPredicate(GroupedPredicate groupedPredicate) {
        return null;
    }

    @Override
    public AstNode renderInListPredicate(InListPredicate inListPredicate) {
        return null;
    }

    @Override
    public AstNode renderInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
        return null;
    }

    @Override
    public AstNode renderInArrayPredicate(InArrayPredicate inArrayPredicate) {
        return null;
    }

    @Override
    public AstNode renderExistsPredicate(ExistsPredicate existsPredicate) {
        return null;
    }

    @Override
    public AstNode renderJunction(Junction junction) {
        return null;
    }

    @Override
    public AstNode renderLikePredicate(LikePredicate likePredicate) {
        return null;
    }

    @Override
    public AstNode renderNegatedPredicate(NegatedPredicate negatedPredicate) {
        return null;
    }

    @Override
    public AstNode renderNullnessPredicate(NullnessPredicate nullnessPredicate) {
        return null;
    }

    @Override
    public AstNode renderThruthnessPredicate(ThruthnessPredicate predicate) {
        return null;
    }

    @Override
    public AstNode renderRelationalPredicate(ComparisonPredicate comparisonPredicate) {
        return null;
    }

    @Override
    public AstNode renderSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
        return null;
    }

    @Override
    public AstNode renderDurationUnit(DurationUnit durationUnit) {
        return null;
    }

    @Override
    public AstNode renderDuration(Duration duration) {
        return null;
    }

    @Override
    public AstNode renderConversion(Conversion conversion) {
        return null;
    }

    @Override
    public AstNode renderStandardTableInsert(TableInsertStandard tableInsert) {
        return null;
    }

    @Override
    public AstNode renderCustomTableInsert(TableInsertCustomSql tableInsert) {
        return null;
    }

    @Override
    public AstNode renderStandardTableDelete(TableDeleteStandard tableDelete) {
        return null;
    }

    @Override
    public AstNode renderCustomTableDelete(TableDeleteCustomSql tableDelete) {
        return null;
    }

    @Override
    public AstNode renderStandardTableUpdate(TableUpdateStandard tableUpdate) {
        return null;
    }

    @Override
    public AstNode renderOptionalTableUpdate(OptionalTableUpdate tableUpdate) {
        return null;
    }

    @Override
    public AstNode renderCustomTableUpdate(TableUpdateCustomSql tableUpdate) {
        return null;
    }

    @Override
    public AstNode renderColumnWriteFragment(ColumnWriteFragment columnWriteFragment) {
        return null;
    }
}
