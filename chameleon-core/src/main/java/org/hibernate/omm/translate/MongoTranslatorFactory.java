package org.hibernate.omm.translate;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

public class MongoTranslatorFactory implements SqlAstTranslatorFactory {

    @Override
    public SqlAstTranslator<JdbcOperationQuerySelect> buildSelectTranslator(
            final SessionFactoryImplementor sessionFactory, final SelectStatement statement) {
        return new MongoSqlAstTranslator<>(sessionFactory, statement);
    }

    @Override
    public SqlAstTranslator<? extends JdbcOperationQueryMutation> buildMutationTranslator(
            final SessionFactoryImplementor sessionFactory, final MutationStatement statement) {
        return new MongoSqlAstTranslator<>(sessionFactory, statement);
    }

    @Override
    public <O extends JdbcMutationOperation> SqlAstTranslator<O> buildModelMutationTranslator(
            final TableMutation<O> mutation, final SessionFactoryImplementor sessionFactory) {
        return new MongoSqlAstTranslator<>(sessionFactory, mutation);
    }
}
