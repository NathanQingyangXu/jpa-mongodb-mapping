package org.hibernate.omm.translate;

import org.hibernate.omm.translate.translator.mongoast.AstNode;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
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

    default AstNode accept(SqlAstNode sqlAstNode) {
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
        } else if (sqlAstNode instanceof TableInsertStandard tableInsertStandard) {
            return renderStandardTableInsert(tableInsertStandard);
        } else if (sqlAstNode instanceof TableInsertCustomSql tableInsertCustomSql) {
            return renderCustomTableInsert(tableInsertCustomSql);
        } else if (sqlAstNode instanceof TableDeleteStandard tableDeleteStandard) {
            return renderStandardTableDelete(tableDeleteStandard);
        } else if (sqlAstNode instanceof TableDeleteCustomSql tableDeleteCustomSql) {
            return renderCustomTableDelete(tableDeleteCustomSql);
        } else if (sqlAstNode instanceof TableUpdateStandard tableUpdateStandard) {
            return renderStandardTableUpdate(tableUpdateStandard);
        } else if (sqlAstNode instanceof OptionalTableUpdate optionalTableUpdate) {
            return renderOptionalTableUpdate(optionalTableUpdate);
        } else if (sqlAstNode instanceof TableUpdateCustomSql tableUpdateCustomSql) {
            return renderCustomTableUpdate(tableUpdateCustomSql);
        } else if (sqlAstNode instanceof ColumnWriteFragment columnWriteFragment) {
            return renderColumnWriteFragment(columnWriteFragment);
        } else {
            throw new IllegalArgumentException("unknown SqlAstNode type: " + sqlAstNode.getClass().getName());
        }
    }

    AstNode renderSelectStatement(SelectStatement statement);

    AstNode renderDeleteStatement(DeleteStatement statement);

    AstNode renderUpdateStatement(UpdateStatement statement);

    AstNode renderInsertStatement(InsertSelectStatement statement);

    AstNode renderAssignment(Assignment assignment);

    AstNode renderQueryGroup(QueryGroup queryGroup);

    AstNode renderQuerySpec(QuerySpec querySpec);

    AstNode renderSortSpecification(SortSpecification sortSpecification);

    AstNode renderOffsetFetchClause(QueryPart queryPart);

    AstNode renderSelectClause(SelectClause selectClause);

    AstNode renderSqlSelection(SqlSelection sqlSelection);

    AstNode renderFromClause(FromClause fromClause);

    AstNode renderTableGroup(TableGroup tableGroup);

    AstNode renderTableGroupJoin(TableGroupJoin tableGroupJoin);

    AstNode renderNamedTableReference(NamedTableReference tableReference);

    AstNode renderValuesTableReference(ValuesTableReference tableReference);

    AstNode renderQueryPartTableReference(QueryPartTableReference tableReference);

    AstNode renderFunctionTableReference(FunctionTableReference tableReference);

    AstNode renderTableReferenceJoin(TableReferenceJoin tableReferenceJoin);

    AstNode renderColumnReference(ColumnReference columnReference);

    AstNode renderNestedColumnReference(NestedColumnReference nestedColumnReference);

    AstNode renderAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression);

    AstNode renderExtractUnit(ExtractUnit extractUnit);

    AstNode renderFormat(Format format);

    AstNode renderDistinct(Distinct distinct);

    AstNode renderOverflow(Overflow overflow);

    AstNode renderStar(Star star);

    AstNode renderTrimSpecification(TrimSpecification trimSpecification);

    AstNode renderCastTarget(CastTarget castTarget);

    AstNode renderBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression);

    AstNode renderCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression);

    AstNode renderCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression);

    AstNode renderAny(Any any);

    AstNode renderEvery(Every every);

    AstNode renderSummarization(Summarization every);

    <T> AstNode renderOver(Over<T> over);

    AstNode renderSelfRenderingExpression(SelfRenderingExpression expression);

    AstNode renderSqlSelectionExpression(SqlSelectionExpression expression);

    AstNode renderEntityTypeLiteral(EntityTypeLiteral expression);

    AstNode renderEmbeddableTypeLiteral(EmbeddableTypeLiteral expression);

    AstNode renderTuple(SqlTuple tuple);

    AstNode renderCollation(Collation collation);

    AstNode renderParameter(JdbcParameter jdbcParameter);

    <T> AstNode renderJdbcLiteral(JdbcLiteral<T> jdbcLiteral);

    <T> AstNode renderQueryLiteral(QueryLiteral<T> queryLiteral);

    <N extends Number> AstNode renderUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal);

    AstNode renderUnaryOperationExpression(UnaryOperation unaryOperationExpression);

    AstNode renderModifiedSubQueryExpression(ModifiedSubQueryExpression expression);

    AstNode renderBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate);

    AstNode renderBetweenPredicate(BetweenPredicate betweenPredicate);

    AstNode renderFilterPredicate(FilterPredicate filterPredicate);
    AstNode renderFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate);
    AstNode renderSqlFragmentPredicate(SqlFragmentPredicate predicate);

    AstNode renderGroupedPredicate(GroupedPredicate groupedPredicate);

    AstNode renderInListPredicate(InListPredicate inListPredicate);

    AstNode renderInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate);

    AstNode renderInArrayPredicate(InArrayPredicate inArrayPredicate);

    AstNode renderExistsPredicate(ExistsPredicate existsPredicate);

    AstNode renderJunction(Junction junction);

    AstNode renderLikePredicate(LikePredicate likePredicate);

    AstNode renderNegatedPredicate(NegatedPredicate negatedPredicate);

    AstNode renderNullnessPredicate(NullnessPredicate nullnessPredicate);

    AstNode renderThruthnessPredicate(ThruthnessPredicate predicate);

    AstNode renderRelationalPredicate(ComparisonPredicate comparisonPredicate);

    AstNode renderSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate);

    AstNode renderDurationUnit(DurationUnit durationUnit);

    AstNode renderDuration(Duration duration);

    AstNode renderConversion(Conversion conversion);


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Model mutations

    AstNode renderStandardTableInsert(TableInsertStandard tableInsert);

    AstNode renderCustomTableInsert(TableInsertCustomSql tableInsert);

    AstNode renderStandardTableDelete(TableDeleteStandard tableDelete);

    AstNode renderCustomTableDelete(TableDeleteCustomSql tableDelete);

    AstNode renderStandardTableUpdate(TableUpdateStandard tableUpdate);

    AstNode renderOptionalTableUpdate(OptionalTableUpdate tableUpdate);

    AstNode renderCustomTableUpdate(TableUpdateCustomSql tableUpdate);

    AstNode renderColumnWriteFragment(ColumnWriteFragment columnWriteFragment);
}
