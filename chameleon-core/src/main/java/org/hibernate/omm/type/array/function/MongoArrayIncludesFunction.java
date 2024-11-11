package org.hibernate.omm.type.array.function;

import org.hibernate.dialect.function.array.AbstractArrayContainsFunction;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

public class MongoArrayIncludesFunction extends AbstractArrayContainsFunction {

    public MongoArrayIncludesFunction(final TypeConfiguration typeConfiguration) {
        super(true, typeConfiguration);
    }

    @Override
    public void render(
            final SqlAppender sqlAppender,
            final List<? extends SqlAstNode> sqlAstArguments,
            final ReturnableType<?> returnType,
            final SqlAstTranslator<?> walker) {
        /*final Expression haystackExpression = (Expression) sqlAstArguments.get(0);
        final Expression needleExpression = (Expression) sqlAstArguments.get(1);

        Attachment mqlAstState = ((AbstractBsonTranslator<?>) walker).getMqlAstState();

        sqlAppender.append("{ ");
        String fieldName = mqlAstState.expect(AttachmentKeys.fieldName(), () -> haystackExpression.accept(walker));
        sqlAppender.append(": { $all: ");
        AstValue fieldValue = mqlAstState.expect(AttachmentKeys.fieldValue(), () -> needleExpression.accept(walker));
        sqlAppender.append(" } }");
        mqlAstState.attach(
                AttachmentKeys.filter(),
                new AstFieldOperationFilter(new AstFilterField(fieldName), new AstAllFilterOperation(fieldValue)));*/
    }
}
