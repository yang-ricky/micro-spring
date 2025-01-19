package org.microspring.orm;

import org.hibernate.Session;

@FunctionalInterface
public interface TransactionCallback<T> {
    T doInTransaction(OrmTemplate template, Session session);
} 