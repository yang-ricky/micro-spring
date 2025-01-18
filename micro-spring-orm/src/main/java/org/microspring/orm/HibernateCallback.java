package org.microspring.orm;

import org.hibernate.Session;

@FunctionalInterface
public interface HibernateCallback<T> {
    T doInHibernate(Session session) throws Exception;
} 