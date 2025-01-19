package org.microspring.orm.transaction;

import org.hibernate.Session;
import org.microspring.orm.OrmTemplate;

@FunctionalInterface
public interface TransactionCallback<T> {
    T doInTransaction(OrmTemplate template, Session session);
} 