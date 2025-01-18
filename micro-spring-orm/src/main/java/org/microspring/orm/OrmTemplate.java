package org.microspring.orm;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 * ORM操作的模板类，封装HibernateTemplate并提供事务管理
 */
public class OrmTemplate {
    
    private static final Logger logger = LoggerFactory.getLogger(OrmTemplate.class);
    private final HibernateTemplate hibernateTemplate;
    private final SessionFactory sessionFactory;

    public OrmTemplate(HibernateTemplate hibernateTemplate) {
        this.hibernateTemplate = hibernateTemplate;
        this.sessionFactory = hibernateTemplate.getSessionFactory();
    }

    public void save(Object entity) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            hibernateTemplate.save(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Failed to save entity", e);
        }
    }

    public <T> T get(Class<T> entityClass, Serializable id) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            T entity = hibernateTemplate.get(entityClass, id);
            tx.commit();
            return entity;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Failed to get entity", e);
        }
    }

    public <T> List<T> find(String hql, Object... params) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            List<T> results = hibernateTemplate.find(hql, params);
            tx.commit();
            return results;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Failed to execute query", e);
        }
    }

    public void update(Object entity) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            hibernateTemplate.update(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Failed to update entity", e);
        }
    }

    public void delete(Object entity) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            hibernateTemplate.delete(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Failed to delete entity", e);
        }
    }

    public <T> T executeInTransaction(TransactionCallback<T> action) {
        return executeInTransaction(action, java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }

    public <T> T executeInTransaction(TransactionCallback<T> action, int isolationLevel) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            // 设置事务隔离级别
            session.doWork(connection -> connection.setTransactionIsolation(isolationLevel));
            
            T result = action.doInTransaction(this, session);
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Transaction failed", e);
        }
    }
}

@FunctionalInterface
interface TransactionCallback<T> {
    T doInTransaction(OrmTemplate template, Session session);
} 