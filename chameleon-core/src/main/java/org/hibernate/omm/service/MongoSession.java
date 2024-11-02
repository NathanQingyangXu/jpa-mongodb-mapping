package org.hibernate.omm.service;

import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;

public class MongoSession extends SessionDelegatorBaseImpl implements SessionImplementor {

    private ReadConcern readConcern;
    private WriteConcern writeConcern;

    private TransactionOptions transactionOptions;

    public TransactionOptions getTransactionOptions() {
        return transactionOptions;
    }

    public MongoSession(SessionImplementor delegate) {
        super(delegate);
    }

    public void setReadConcern(ReadConcern readConcern) {
        this.readConcern = readConcern;
    }

    public void setWriteConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
    }

    @Override
    public Transaction beginTransaction() {
        return beginTransaction(TransactionOptions.builder()
                .readConcern(readConcern)
                .writeConcern(writeConcern)
                .build());
    }

    public Transaction beginTransaction(TransactionOptions options) {
        transactionOptions = options;
        return super.beginTransaction();
    }
}
