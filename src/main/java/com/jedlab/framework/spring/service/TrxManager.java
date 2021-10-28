package com.jedlab.framework.spring.service;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public class TrxManager
{

    PlatformTransactionManager ptm;
    TransactionTemplate transactionManager;

    public TrxManager(PlatformTransactionManager ptm)
    {
        this.transactionManager = new TransactionTemplate(ptm);
    }

    public void doInTransaction(Trx trx)
    {
        transactionManager.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status)
            {
                trx.doInTransaction(status);
            }
        });
    }

    public <T> T execute(TransactionCallback<T> callback)
    {
        return transactionManager.execute(callback);
    }
    
   

}
