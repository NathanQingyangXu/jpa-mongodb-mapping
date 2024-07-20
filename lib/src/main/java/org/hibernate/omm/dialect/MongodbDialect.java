package org.hibernate.omm.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.omm.util.StringUtil;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

public class MongodbDialect extends Dialect {

	public MongodbDialect() {
		super();
	}

	public MongodbDialect(DialectResolutionInfo dialectResolutionInfo) {
		super(dialectResolutionInfo);
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {

		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new MongodbSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public void appendLiteral(SqlAppender appender, String literal) {
		appender.appendSql( StringUtil.writeStringHelper( literal ) );
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public boolean supportsStandardArrays() {
		return true;
	}
}
