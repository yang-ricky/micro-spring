package org.microspring.orm.repository.support;

import org.hibernate.Session;

@FunctionalInterface
public interface SessionCallback<T> {
    T doInSession(Session session) throws Exception;
} 