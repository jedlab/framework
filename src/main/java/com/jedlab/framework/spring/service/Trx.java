package com.jedlab.framework.spring.service;

import org.springframework.transaction.TransactionStatus;

public interface Trx {

	void doInTransaction(TransactionStatus status);

}
