package org.hibernate.omm.translate.translator;

import static org.hibernate.omm.translate.translator.AttachmentKeys.fieldName;
import static org.hibernate.omm.translate.translator.AttachmentKeys.filter;

import java.io.StringWriter;
import org.bson.BsonNull;
import org.bson.json.JsonWriter;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.omm.exception.NotSupportedRuntimeException;
import org.hibernate.omm.translate.AbstractSqlAstTranslator;
import org.hibernate.omm.translate.translator.mongoast.AstLiteralValue;
import org.hibernate.omm.translate.translator.mongoast.AstNode;
import org.hibernate.omm.translate.translator.mongoast.AstPlaceholder;
import org.hibernate.omm.translate.translator.mongoast.AstValue;
import org.hibernate.omm.translate.translator.mongoast.filters.AstComparisonFilterOperation;
import org.hibernate.omm.translate.translator.mongoast.filters.AstComparisonFilterOperator;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFieldOperationFilter;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFilterField;
import org.hibernate.omm.translate.translator.mongoast.filters.AstMatchesEverythingFilter;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Contains common stuff shared between {@link MQLTranslator} and {@link BsonCommandTranslator}, e.g.:
 * <ul>
 *     <li>expression rendering</li>
 *     <li>predicate rendering</li>
 *     <li>util methods</li>
 * </ul>
 *
 * @author Nathan Xu
 * @since 1.0.0
 * @param <T> {@link JdbcOperation} generic type
 */
public class AbstractBsonTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

    protected AstNode root;
    protected Attachment mqlAstState = new Attachment();
    private boolean inAggregateExpressionScope;

    public AbstractBsonTranslator(final SessionFactoryImplementor sessionFactory, final Statement statement) {
        super(sessionFactory, statement);
    }

    public Attachment getMqlAstState() {
        return mqlAstState;
    }

    @Override
    protected String getSql() {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        root.render(jsonWriter);
        return writer.toString();
    }

    public boolean isInAggregateExpressionScope() {
        return inAggregateExpressionScope;
    }

    public void setInAggregateExpressionScope(final boolean inAggregateExpressionScope) {
        this.inAggregateExpressionScope = inAggregateExpressionScope;
    }

    @Override
    protected void renderDmlTargetTableExpression(final NamedTableReference tableReference) {
        registerAffectedTable(tableReference);
    }

    @Override
    protected boolean renderNamedTableReference(final NamedTableReference tableReference, final LockMode lockMode) {
        registerAffectedTable(tableReference);
        return false;
    }

    @Override
    public void visitNamedTableReference(final NamedTableReference tableReference) {
        mqlAstState.attach(AttachmentKeys.collectionName(), tableReference.getTableExpression());
    }

    @Override
    protected void renderParameterAsParameter(final int position, final JdbcParameter jdbcParameter) {
        final JdbcType jdbcType =
                jdbcParameter.getExpressionType().getJdbcMapping(0).getJdbcType();
        assert jdbcType != null;
        final String parameterMarker = parameterMarkerStrategy.createMarker(position, jdbcType);
        jdbcType.appendWriteExpression(parameterMarker, this, dialect);
        mqlAstState.attach(AttachmentKeys.fieldValue(), new AstPlaceholder());
    }

    @Override
    protected void visitWhereClause(final Predicate whereClauseRestrictions) {
        final Predicate additionalWherePredicate = this.additionalWherePredicate;
        final boolean existsWhereClauseRestrictions =
                whereClauseRestrictions != null && !whereClauseRestrictions.isEmpty();
        final boolean existsAdditionalWherePredicate = additionalWherePredicate != null;
        if (existsWhereClauseRestrictions || existsAdditionalWherePredicate) {
            // TODO: entered but untested branch
            getClauseStack().push(Clause.WHERE);
            try {
                if (existsWhereClauseRestrictions) {
                    whereClauseRestrictions.accept(this);
                }
                if (additionalWherePredicate != null) {
                    throw new UnsupportedOperationException();
                    //                    this.additionalWherePredicate = null;
                    //                    additionalWherePredicate.accept(this);
                }
            } finally {
                getClauseStack().pop();
            }
        } else {
            mqlAstState.attach(AttachmentKeys.filter(), new AstMatchesEverythingFilter());
        }
    }

    @Override
    public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
        Expression expression = nullnessPredicate.getExpression();
        if (SqlTupleContainer.getSqlTuple(expression) != null) {
            throw new NotSupportedRuntimeException();
        }

        String fieldName = mqlAstState.expect(fieldName(), () -> expression.accept(this));

        mqlAstState.attach(
                filter(),
                new AstFieldOperationFilter(
                        new AstFilterField(fieldName),
                        new AstComparisonFilterOperation(
                                nullnessPredicate.isNegated()
                                        ? AstComparisonFilterOperator.NE
                                        : AstComparisonFilterOperator.EQ,
                                new AstLiteralValue(BsonNull.VALUE))));
    }

    @Override
    public void visitInListPredicate(final InListPredicate inListPredicate) {
        throw new NotSupportedRuntimeException();
    }

    @Override
    public void visitExistsPredicate(final ExistsPredicate existsPredicate) {
        throw new NotSupportedRuntimeException();
    }

    @Override
    public void visitJunction(final Junction junction) {
        throw new NotSupportedRuntimeException();
    }

    @Override
    public void visitNegatedPredicate(final NegatedPredicate negatedPredicate) {
        throw new NotSupportedRuntimeException();
    }

    protected void renderComparisonStandard(
            final Expression lhs, final ComparisonOperator operator, final Expression rhs) {
        if (inAggregateExpressionScope) {
            throw new NotSupportedRuntimeException();
        }
        String fieldName = mqlAstState.expect(AttachmentKeys.fieldName(), () -> lhs.accept(this));

        AstValue value = mqlAstState.expect(AttachmentKeys.fieldValue(), () -> rhs.accept(this));
        mqlAstState.attach(
                AttachmentKeys.filter(),
                new AstFieldOperationFilter(
                        new AstFilterField(fieldName),
                        new AstComparisonFilterOperation(convertOperator(operator), value)));
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
