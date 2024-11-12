package org.hibernate.omm.service;

import static org.hibernate.internal.TransactionManagement.manageTransaction;

import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import java.util.function.Consumer;
import org.hibernate.Session;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.engine.spi.SessionFactoryDelegatingImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public class MongoSessionFactory extends SessionFactoryDelegatingImpl implements SessionFactoryImplementor {

  private ReadConcern readConcern;
  private WriteConcern writeConcern;

  public MongoSessionFactory(SessionFactoryImplementor delegate) {
    super(delegate);
  }

  public void setReadConcern(ReadConcern readConcern) {
    this.readConcern = readConcern;
  }

  public void setWriteConcern(WriteConcern writeConcern) {
    this.writeConcern = writeConcern;
  }

  @Override
  public MongoSession openSession() {
    return openSession(readConcern, writeConcern);
  }

  public MongoSession openSession(ReadConcern sessionReadConcern, WriteConcern sessionWriteConcern) {
    MongoSession mongoSession = new MongoSession(super.openSession());
    ThreadLocalSessionContext.bind(mongoSession);
    mongoSession.setReadConcern(sessionReadConcern == null ? readConcern : sessionReadConcern);
    mongoSession.setWriteConcern(sessionWriteConcern == null ? writeConcern : sessionWriteConcern);
    return mongoSession;
  }

  @Override
  public void inSession(Consumer<Session> action) {
    inSession(readConcern, writeConcern, action);
  }

  public void inSession(ReadConcern sessionReadConcern, WriteConcern sessionWriteConcern, Consumer<Session> action) {
    try ( Session session = openSession(sessionReadConcern, sessionWriteConcern) ) {
      action.accept( session );
    }
  }

  @Override
  public void inTransaction(Consumer<Session> action) {
      inTransaction(
          TransactionOptions.builder()
              .readConcern(readConcern)
              .writeConcern(writeConcern)
              .build(),
              action);
  }

  public void inTransaction(TransactionOptions transactionOptions, Consumer<Session> action) {
    inSession( session -> manageTransaction( session, ((MongoSession) session).beginTransaction(transactionOptions), action ) );
  }

}
