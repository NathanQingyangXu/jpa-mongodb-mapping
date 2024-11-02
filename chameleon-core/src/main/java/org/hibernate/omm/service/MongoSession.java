package org.hibernate.omm.service;

import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;

public class MongoSession extends SessionDelegatorBaseImpl implements SessionImplementor {

    public static ThreadLocal<TransactionOptions> transactionOptionsThreadLocal = new ThreadLocal<>();

    private ReadConcern defaultTransactionReadConcern;
    private WriteConcern defaultTransactionWriteConcern;

    public MongoSession(SessionImplementor delegate) {
        super(delegate);
    }

    public void setDefaultTransactionReadConcern(ReadConcern defaultTransactionReadConcern) {
        this.defaultTransactionReadConcern = defaultTransactionReadConcern;
    }

    public void setDefaultTransactionWriteConcern(WriteConcern defaultTransactionWriteConcern) {
        this.defaultTransactionWriteConcern = defaultTransactionWriteConcern;
    }

    public Transaction beginTransaction() {
        return beginTransaction(defaultTransactionReadConcern, defaultTransactionWriteConcern);
    }

    public Transaction beginTransaction(ReadConcern transactionReadConcern, WriteConcern transactionWriteConcern) {
      var transactionOptions = TransactionOptions.builder()
          .readConcern(transactionReadConcern)
          .writeConcern(transactionWriteConcern)
          .build();
      transactionOptionsThreadLocal.set(transactionOptions);
        return super.beginTransaction();
    }
}
