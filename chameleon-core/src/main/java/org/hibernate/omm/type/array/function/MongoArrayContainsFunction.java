package org.hibernate.omm.type.array.function;

import java.util.List;
import org.hibernate.dialect.function.array.AbstractArrayContainsFunction;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.omm.translate.translator.AbstractBsonTranslator;
import org.hibernate.omm.translate.translator.Attachment;
import org.hibernate.omm.translate.translator.AttachmentKeys;
import org.hibernate.omm.translate.translator.mongoast.AstValue;
import org.hibernate.omm.translate.translator.mongoast.filters.AstAllFilterOperation;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFieldOperationFilter;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFilterField;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.spi.TypeConfiguration;

public class MongoArrayContainsFunction extends AbstractArrayContainsFunction {

    public MongoArrayContainsFunction(final TypeConfiguration typeConfiguration) {
        super(true, typeConfiguration);
    }

    @Override
    public void render(
            final SqlAppender sqlAppender,
            final List<? extends SqlAstNode> sqlAstArguments,
            final ReturnableType<?> returnType,
            final SqlAstTranslator<?> walker) {
        final Expression haystackExpression = (Expression) sqlAstArguments.get(0);
        final Expression needleExpression = (Expression) sqlAstArguments.get(1);

        final JdbcMappingContainer needleTypeContainer = needleExpression.getExpressionType();
        final JdbcMapping needleType = needleTypeContainer == null ? null : needleTypeContainer.getSingleJdbcMapping();

        Attachment mqlAstState = ((AbstractBsonTranslator<?>) walker).getMqlAstState();

        if (needleType == null || needleType instanceof BasicPluralType<?, ?>) {
            sqlAppender.append("{ ");
            String fieldName = mqlAstState.expect(AttachmentKeys.fieldName(), () -> haystackExpression.accept(walker));
            sqlAppender.append(": { $all: ");
            AstValue fieldValue =
                    mqlAstState.expect(AttachmentKeys.fieldValue(), () -> needleExpression.accept(walker));
            sqlAppender.append(" } }");
            mqlAstState.attach(
                    AttachmentKeys.filter(),
                    new AstFieldOperationFilter(new AstFilterField(fieldName), new AstAllFilterOperation(fieldValue)));
        } else {
            // TODO: this is not tested, so no MQL AST support yet
            sqlAppender.append("{ ");
            haystackExpression.accept(walker);
            sqlAppender.append(": ");
            needleExpression.accept(walker);
            sqlAppender.append(" }");
        }
    }
}
