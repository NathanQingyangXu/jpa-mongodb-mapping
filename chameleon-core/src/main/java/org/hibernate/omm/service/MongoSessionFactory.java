package org.hibernate.omm.service;

import static org.hibernate.internal.TransactionManagement.manageTransaction;

import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import java.util.function.Consumer;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryDelegatingImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public class MongoSessionFactory extends SessionFactoryDelegatingImpl implements SessionFactoryImplementor {

  private SessionFactoryImplementor delegate;

  private ReadConcern defaultSessionReadConcern;
  private WriteConcern defaultSessionWriteConcern;

  public MongoSessionFactory(SessionFactoryImplementor delegate) {
    super(delegate);
  }

  public void setDefaultSessionReadConcern(ReadConcern defaultSessionReadConcern) {
    this.defaultSessionReadConcern = defaultSessionReadConcern;
  }

  public void setDefaultSessionWriteConcern(WriteConcern defaultSessionWriteConcern) {
    this.defaultSessionWriteConcern = defaultSessionWriteConcern;
  }

  @Override
  public MongoSession openSession() {
    return openSession(defaultSessionReadConcern, defaultSessionWriteConcern);
  }

  public MongoSession openSession(ReadConcern readConcern, WriteConcern writeConcern) {
    MongoSession mongoSession = new MongoSession(delegate.openSession());
    mongoSession.setDefaultTransactionReadConcern(defaultSessionReadConcern);
    mongoSession.setDefaultTransactionWriteConcern(defaultSessionWriteConcern);
    if (readConcern != null) {
      mongoSession.setDefaultTransactionReadConcern(readConcern);
    }
    if (writeConcern != null) {
      mongoSession.setDefaultTransactionWriteConcern(writeConcern);
    }
    return mongoSession;
  }

  @Override
  public void inTransaction(Consumer<Session> action) {
    inTransaction(action, defaultSessionReadConcern, defaultSessionWriteConcern);
  }

  public void inTransaction(Consumer<Session> action, ReadConcern readConcern, WriteConcern writeConcern) {
    inSession( session -> manageTransaction( session, ((MongoSession) session).beginTransaction(readConcern, writeConcern), action ) );
  }

}
