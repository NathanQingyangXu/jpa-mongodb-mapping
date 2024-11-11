package org.hibernate.omm.translate;

import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.sqm.sql.internal.BasicValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.query.sqm.tree.expression.Conversion;
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
import org.hibernate.sql.exec.internal.AbstractJdbcParameter;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.internal.*;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

public interface MqlAstWalker {

    default Object accept(Statement statement) {
        if (statement instanceof TableInsertStandard tableInsertStandard) {
            return renderStandardTableInsert(tableInsertStandard);
        } else if (statement instanceof TableInsertCustomSql tableInsertCustomSql) {
            return renderCustomTableInsert(tableInsertCustomSql);
        } else if (statement instanceof TableDeleteStandard tableDeleteStandard) {
            return renderStandardTableDelete(tableDeleteStandard);
        } else if (statement instanceof TableDeleteCustomSql tableDeleteCustomSql) {
            return renderCustomTableDelete(tableDeleteCustomSql);
        } else if (statement instanceof TableUpdateStandard tableUpdateStandard) {
            return renderStandardTableUpdate(tableUpdateStandard);
        } else if (statement instanceof OptionalTableUpdate optionalTableUpdate) {
            return renderOptionalTableUpdate(optionalTableUpdate);
        } else if (statement instanceof TableUpdateCustomSql tableUpdateCustomSql) {
            return renderCustomTableUpdate(tableUpdateCustomSql);
        } else {
            throw new IllegalArgumentException("unknown Statement type: " + statement.getClass().getName());
        }
    }

    default Object accept(SqlAstNode sqlAstNode) {
        if (sqlAstNode instanceof SelectStatement selectStatement) {
            return renderSelectStatement(selectStatement);
        } else if (sqlAstNode instanceof DeleteStatement deleteStatement) {
            return renderDeleteStatement(deleteStatement);
        } else if (sqlAstNode instanceof UpdateStatement updateStatement) {
            return renderUpdateStatement(updateStatement);
        } else if (sqlAstNode instanceof InsertSelectStatement insertSelectStatement) {
            return renderInsertStatement(insertSelectStatement);
        } else if (sqlAstNode instanceof Assignment assignment) {
            return renderAssignment(assignment);
        } else if (sqlAstNode instanceof QueryGroup queryGroup) {
            return renderQueryGroup(queryGroup);
        } else if (sqlAstNode instanceof QuerySpec querySpec) {
            return renderQuerySpec(querySpec);
        } else if (sqlAstNode instanceof SortSpecification sortSpecification) {
            return renderSortSpecification(sortSpecification);
        } else if (sqlAstNode instanceof SelectClause selectClause) {
            return renderSelectClause(selectClause);
        } else if (sqlAstNode instanceof SqlSelectionImpl sqlSelectionimpl) {
            return accept(sqlSelectionimpl.getExpression());
        } else if (sqlAstNode instanceof ResultSetMappingSqlSelection) {
            throw new UnsupportedOperationException();
        } else if (sqlAstNode instanceof FromClause fromClause) {
            return renderFromClause(fromClause);
        } else if (sqlAstNode instanceof TableGroup tableGroup) {
            return renderTableGroup(tableGroup);
        } else if (sqlAstNode instanceof TableGroupJoin tableGroupJoin) {
            return renderTableGroupJoin(tableGroupJoin);
        } else if (sqlAstNode instanceof NamedTableReference namedTableReference) {
            return renderNamedTableReference(namedTableReference);
        } else if (sqlAstNode instanceof ValuesTableReference valuesTableReference) {
            return renderValuesTableReference(valuesTableReference);
        } else if (sqlAstNode instanceof QueryPartTableReference queryPartTableReference) {
            return renderQueryPartTableReference(queryPartTableReference);
        } else if (sqlAstNode instanceof FunctionTableReference functionTableReference) {
            return accept(functionTableReference.getFunctionExpression());
        } else if (sqlAstNode instanceof TableReferenceJoin tableReferenceJoin) {
            return renderTableReferenceJoin(tableReferenceJoin);
        } else if (sqlAstNode instanceof NestedColumnReference nestedColumnReference) {
            return renderNestedColumnReference(nestedColumnReference);
        } else if (sqlAstNode instanceof ColumnReference columnReference) {
            return renderColumnReference(columnReference);
        } else if (sqlAstNode instanceof AggregateColumnWriteExpression aggregateColumnWriteExpression) {
            return renderAggregateColumnWriteExpression(aggregateColumnWriteExpression);
        } else if (sqlAstNode instanceof ExtractUnit extractUnit) {
            return renderExtractUnit(extractUnit);
        } else if (sqlAstNode instanceof Format format) {
            return renderFormat(format);
        } else if (sqlAstNode instanceof Distinct distinct) {
            return renderDistinct(distinct);
        } else if (sqlAstNode instanceof Overflow overflow) {
            return renderOverflow(overflow);
        } else if (sqlAstNode instanceof Star star) {
            return renderStar(star);
        } else if (sqlAstNode instanceof TrimSpecification trimSpecification) {
            return renderTrimSpecification(trimSpecification);
        } else if (sqlAstNode instanceof CastTarget castTarget) {
            return renderCastTarget(castTarget);
        } else if (sqlAstNode instanceof BinaryArithmeticExpression binaryArithmeticExpression) {
            return renderBinaryArithmeticExpression(binaryArithmeticExpression);
        } else if (sqlAstNode instanceof CaseSearchedExpression caseSearchedExpression) {
            return renderCaseSearchedExpression(caseSearchedExpression);
        } else if (sqlAstNode instanceof CaseSimpleExpression caseSimpleExpression) {
            return renderCaseSimpleExpression(caseSimpleExpression);
        } else if (sqlAstNode instanceof Any any) {
            return renderAny(any);
        } else if (sqlAstNode instanceof Every every) {
            return renderEvery(every);
        } else if (sqlAstNode instanceof Summarization summarization) {
            return renderSummarization(summarization);
        } else if (sqlAstNode instanceof Over<?> over) {
            return renderOver(over);
        } else if (sqlAstNode instanceof SelfRenderingExpression selfRenderingExpression) {
            return renderSelfRenderingExpression(selfRenderingExpression);
        } else if (sqlAstNode instanceof SqlSelectionExpression sqlSelectionExpression) {
            return renderSqlSelectionExpression(sqlSelectionExpression);
        } else if (sqlAstNode instanceof EntityTypeLiteral entityTypeLiteral) {
            return renderEntityTypeLiteral(entityTypeLiteral);
        } else if (sqlAstNode instanceof EmbeddableTypeLiteral embeddableTypeLiteral) {
            return renderEmbeddableTypeLiteral(embeddableTypeLiteral);
        } else if (sqlAstNode instanceof SqlTuple sqlTuple) {
            return renderTuple(sqlTuple);
        } else if (sqlAstNode instanceof Collation collation) {
            return renderCollation(collation);
        } else if (sqlAstNode instanceof AbstractJdbcParameter abstractJdbcParameter) {
            return renderParameter(abstractJdbcParameter);
        } else if (sqlAstNode instanceof JdbcLiteral<?> jdbcLiteral) {
            return renderJdbcLiteral(jdbcLiteral);
        } else if (sqlAstNode instanceof QueryLiteral<?> queryLiteral) {
            return renderQueryLiteral(queryLiteral);
        } else if (sqlAstNode instanceof UnparsedNumericLiteral<?> unparsedNumericLiteral) {
            return renderUnparsedNumericLiteral(unparsedNumericLiteral);
        } else if (sqlAstNode instanceof UnaryOperation unaryOperation) {
            return renderUnaryOperationExpression(unaryOperation);
        } else if (sqlAstNode instanceof ModifiedSubQueryExpression modifiedSubQueryExpression) {
            return renderModifiedSubQueryExpression(modifiedSubQueryExpression);
        } else if (sqlAstNode instanceof BooleanExpressionPredicate booleanExpressionPredicate) {
            return renderBooleanExpressionPredicate(booleanExpressionPredicate);
        } else if (sqlAstNode instanceof BetweenPredicate betweenPredicate) {
            return renderBetweenPredicate(betweenPredicate);
        } else if (sqlAstNode instanceof FilterPredicate filterPredicate) {
            return renderFilterPredicate(filterPredicate);
        } else if (sqlAstNode instanceof FilterPredicate.FilterFragmentPredicate filterFragmentPredicate) {
            return renderFilterFragmentPredicate(filterFragmentPredicate);
        } else if (sqlAstNode instanceof SqlFragmentPredicate sqlFragmentPredicate) {
            return renderSqlFragmentPredicate(sqlFragmentPredicate);
        } else if (sqlAstNode instanceof GroupedPredicate groupedPredicate) {
            return renderGroupedPredicate(groupedPredicate);
        } else if (sqlAstNode instanceof InListPredicate inListPredicate) {
            return renderInListPredicate(inListPredicate);
        } else if (sqlAstNode instanceof InSubQueryPredicate inSubQueryPredicate) {
            return renderInSubQueryPredicate(inSubQueryPredicate);
        } else if (sqlAstNode instanceof InArrayPredicate inArrayPredicate) {
            return renderInArrayPredicate(inArrayPredicate);
        } else if (sqlAstNode instanceof ExistsPredicate existsPredicate) {
            return renderExistsPredicate(existsPredicate);
        } else if (sqlAstNode instanceof Junction junction) {
            return renderJunction(junction);
        } else if (sqlAstNode instanceof LikePredicate likePredicate) {
            return renderLikePredicate(likePredicate);
        } else if (sqlAstNode instanceof NegatedPredicate negatedPredicate) {
            return renderNegatedPredicate(negatedPredicate);
        } else if (sqlAstNode instanceof NullnessPredicate nullnessPredicate) {
            return renderNullnessPredicate(nullnessPredicate);
        } else if (sqlAstNode instanceof ThruthnessPredicate thruthnessPredicate) {
            return renderThruthnessPredicate(thruthnessPredicate);
        } else if (sqlAstNode instanceof ComparisonPredicate comparisonPredicate) {
            return renderRelationalPredicate(comparisonPredicate);
        } else if (sqlAstNode instanceof SelfRenderingPredicate selfRenderingPredicate) {
            return renderSelfRenderingPredicate(selfRenderingPredicate);
        } else if (sqlAstNode instanceof DurationUnit durationUnit) {
            return renderDurationUnit(durationUnit);
        } else if (sqlAstNode instanceof Duration duration) {
            return renderDuration(duration);
        } else if (sqlAstNode instanceof Conversion conversion) {
            return renderConversion(conversion);
        } else if (sqlAstNode instanceof ColumnWriteFragment columnWriteFragment) {
            return renderColumnWriteFragment(columnWriteFragment);
        } else if (sqlAstNode instanceof BasicValuedPathInterpretation basicValuedPathInterpretation) {
            return renderColumnReference(basicValuedPathInterpretation.getColumnReference());
        } else if (sqlAstNode instanceof SqmParameterInterpretation sqmParameterInterpretation) {
            return accept(sqmParameterInterpretation.getResolvedExpression());
        } else {
            throw new IllegalArgumentException("unknown SqlAstNode type: " + sqlAstNode.getClass().getName());
        }
    }

    Object renderSelectStatement(SelectStatement statement);

    Object renderDeleteStatement(DeleteStatement statement);

    Object renderUpdateStatement(UpdateStatement statement);

    Object renderInsertStatement(InsertSelectStatement statement);

    Object renderAssignment(Assignment assignment);

    Object renderQueryGroup(QueryGroup queryGroup);

    Object renderQuerySpec(QuerySpec querySpec);

    Object renderSortSpecification(SortSpecification sortSpecification);

    Object renderOffsetFetchClause(QueryPart queryPart);

    Object renderSelectClause(SelectClause selectClause);

    Object renderSqlSelection(SqlSelection sqlSelection);

    Object renderFromClause(FromClause fromClause);

    Object renderTableGroup(TableGroup tableGroup);

    Object renderTableGroupJoin(TableGroupJoin tableGroupJoin);

    Object renderNamedTableReference(NamedTableReference tableReference);

    Object renderValuesTableReference(ValuesTableReference tableReference);

    Object renderQueryPartTableReference(QueryPartTableReference tableReference);

    Object renderFunctionTableReference(FunctionTableReference tableReference);

    Object renderTableReferenceJoin(TableReferenceJoin tableReferenceJoin);

    Object renderColumnReference(ColumnReference columnReference);

    Object renderNestedColumnReference(NestedColumnReference nestedColumnReference);

    Object renderAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression);

    Object renderExtractUnit(ExtractUnit extractUnit);

    Object renderFormat(Format format);

    Object renderDistinct(Distinct distinct);

    Object renderOverflow(Overflow overflow);

    Object renderStar(Star star);

    Object renderTrimSpecification(TrimSpecification trimSpecification);

    Object renderCastTarget(CastTarget castTarget);

    Object renderBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression);

    Object renderCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression);

    Object renderCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression);

    Object renderAny(Any any);

    Object renderEvery(Every every);

    Object renderSummarization(Summarization every);

    <T> Object renderOver(Over<T> over);

    Object renderSelfRenderingExpression(SelfRenderingExpression expression);

    Object renderSqlSelectionExpression(SqlSelectionExpression expression);

    Object renderEntityTypeLiteral(EntityTypeLiteral expression);

    Object renderEmbeddableTypeLiteral(EmbeddableTypeLiteral expression);

    Object renderTuple(SqlTuple tuple);

    Object renderCollation(Collation collation);

    Object renderParameter(JdbcParameter jdbcParameter);

    <T> Object renderJdbcLiteral(JdbcLiteral<T> jdbcLiteral);

    <T> Object renderQueryLiteral(QueryLiteral<T> queryLiteral);

    <N extends Number> Object renderUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal);

    Object renderUnaryOperationExpression(UnaryOperation unaryOperationExpression);

    Object renderModifiedSubQueryExpression(ModifiedSubQueryExpression expression);

    Object renderBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate);

    Object renderBetweenPredicate(BetweenPredicate betweenPredicate);

    Object renderFilterPredicate(FilterPredicate filterPredicate);
    Object renderFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate);
    Object renderSqlFragmentPredicate(SqlFragmentPredicate predicate);

    Object renderGroupedPredicate(GroupedPredicate groupedPredicate);

    Object renderInListPredicate(InListPredicate inListPredicate);

    Object renderInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate);

    Object renderInArrayPredicate(InArrayPredicate inArrayPredicate);

    Object renderExistsPredicate(ExistsPredicate existsPredicate);

    Object renderJunction(Junction junction);

    Object renderLikePredicate(LikePredicate likePredicate);

    Object renderNegatedPredicate(NegatedPredicate negatedPredicate);

    Object renderNullnessPredicate(NullnessPredicate nullnessPredicate);

    Object renderThruthnessPredicate(ThruthnessPredicate predicate);

    Object renderRelationalPredicate(ComparisonPredicate comparisonPredicate);

    Object renderSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate);

    Object renderDurationUnit(DurationUnit durationUnit);

    Object renderDuration(Duration duration);

    Object renderConversion(Conversion conversion);


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Model mutations

    Object renderStandardTableInsert(TableInsertStandard tableInsert);

    Object renderCustomTableInsert(TableInsertCustomSql tableInsert);

    Object renderStandardTableDelete(TableDeleteStandard tableDelete);

    Object renderCustomTableDelete(TableDeleteCustomSql tableDelete);

    Object renderStandardTableUpdate(TableUpdateStandard tableUpdate);

    Object renderOptionalTableUpdate(OptionalTableUpdate tableUpdate);

    Object renderCustomTableUpdate(TableUpdateCustomSql tableUpdate);

    Object renderColumnWriteFragment(ColumnWriteFragment columnWriteFragment);
}
