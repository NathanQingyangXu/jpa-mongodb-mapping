package org.hibernate.omm.boot;

import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.omm.jdbc.MongoConnectionProvider;
import org.hibernate.omm.service.MongoSessionFactory;

public class MongoSessionFactoryBuilderImpl extends AbstractDelegatingSessionFactoryBuilderImplementor<SessionFactoryBuilderImplementor>
    implements SessionFactoryBuilderImplementor {

  public MongoSessionFactoryBuilderImpl(SessionFactoryBuilderImplementor delegate) {
    super( delegate );
  }

  @Override
  protected SessionFactoryBuilderImplementor getThis() {
    return this;
  }

  @Override
  public MongoSessionFactory build() {
    SessionFactoryImplementor delegate = (SessionFactoryImplementor) super.build();
    MongoConnectionProvider mongoConnectionProvider = delegate.getServiceRegistry().getService(MongoConnectionProvider.class);
    MongoSessionFactory mongoSessionFactory = new MongoSessionFactory(delegate);
    mongoConnectionProvider.setSessionFactory(mongoSessionFactory);
    return mongoSessionFactory;
  }
}

