package org.hibernate.omm.translate;

import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.*;
import org.hibernate.sql.ast.tree.from.*;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.*;
import org.hibernate.sql.ast.tree.select.*;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.internal.*;

abstract class AbstractSqlAstWalker implements SqlAstWalker {
    @Override
    public void visitSelectStatement(SelectStatement statement) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitDeleteStatement(DeleteStatement statement) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitUpdateStatement(UpdateStatement statement) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitInsertStatement(InsertSelectStatement statement) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitAssignment(Assignment assignment) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitQueryGroup(QueryGroup queryGroup) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitQuerySpec(QuerySpec querySpec) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitSortSpecification(SortSpecification sortSpecification) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitOffsetFetchClause(QueryPart querySpec) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitSelectClause(SelectClause selectClause) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitSqlSelection(SqlSelection sqlSelection) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitFromClause(FromClause fromClause) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitTableGroup(TableGroup tableGroup) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitNamedTableReference(NamedTableReference tableReference) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitValuesTableReference(ValuesTableReference tableReference) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitFunctionTableReference(FunctionTableReference tableReference) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitColumnReference(ColumnReference columnReference) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitNestedColumnReference(NestedColumnReference nestedColumnReference) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitExtractUnit(ExtractUnit extractUnit) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitFormat(Format format) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitDistinct(Distinct distinct) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitOverflow(Overflow overflow) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitStar(Star star) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitTrimSpecification(TrimSpecification trimSpecification) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitCastTarget(CastTarget castTarget) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitAny(Any any) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitEvery(Every every) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitSummarization(Summarization every) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitOver(Over<?> over) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitEntityTypeLiteral(EntityTypeLiteral expression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitEmbeddableTypeLiteral(EmbeddableTypeLiteral expression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitTuple(SqlTuple tuple) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitCollation(Collation collation) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitParameter(JdbcParameter jdbcParameter) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitJdbcLiteral(JdbcLiteral<?> jdbcLiteral) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitQueryLiteral(QueryLiteral<?> queryLiteral) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public <N extends Number> void visitUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitModifiedSubQueryExpression(ModifiedSubQueryExpression expression) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitBetweenPredicate(BetweenPredicate betweenPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitFilterPredicate(FilterPredicate filterPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitSqlFragmentPredicate(SqlFragmentPredicate predicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitGroupedPredicate(GroupedPredicate groupedPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitInListPredicate(InListPredicate inListPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitExistsPredicate(ExistsPredicate existsPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitJunction(Junction junction) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitLikePredicate(LikePredicate likePredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitNegatedPredicate(NegatedPredicate negatedPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitThruthnessPredicate(ThruthnessPredicate predicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitDurationUnit(DurationUnit durationUnit) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitDuration(Duration duration) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitConversion(Conversion conversion) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitStandardTableInsert(TableInsertStandard tableInsert) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitCustomTableInsert(TableInsertCustomSql tableInsert) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitStandardTableDelete(TableDeleteStandard tableDelete) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitCustomTableDelete(TableDeleteCustomSql tableDelete) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitStandardTableUpdate(TableUpdateStandard tableUpdate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitOptionalTableUpdate(OptionalTableUpdate tableUpdate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitCustomTableUpdate(TableUpdateCustomSql tableUpdate) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public void visitColumnWriteFragment(ColumnWriteFragment columnWriteFragment) {
        throw new IllegalStateException("should not be called");
    }
}
