package org.microspring.orm;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.microspring.jdbc.transaction.JdbcTransactionManager;
import org.microspring.transaction.TransactionStatus;
import org.microspring.transaction.support.DefaultTransactionDefinition;
import java.io.Serializable;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HibernateTemplate {
    
    private static final Logger logger = LoggerFactory.getLogger(HibernateTemplate.class);
    private final SessionFactory sessionFactory;
    private JdbcTransactionManager transactionManager;

    public HibernateTemplate(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setTransactionManager(JdbcTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public <T> T execute(HibernateCallback<T> action) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            try {
                T result = action.doInHibernate(session);
                session.getTransaction().commit();
                return result;
            } catch (Exception e) {
                if (session.getTransaction() != null && session.getTransaction().isActive()) {
                    session.getTransaction().rollback();
                }
                logger.error("Error executing Hibernate action", e);
                throw new RuntimeException("Error executing Hibernate action", e);
            }
        }
    }

    public void save(Object entity) {
        try {
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
            Session session = sessionFactory.getCurrentSession();
            try {
                session.beginTransaction();
                session.save(entity);
                session.getTransaction().commit();
                transactionManager.commit(status);
            } catch (Exception e) {
                if (session.getTransaction() != null && session.getTransaction().isActive()) {
                    session.getTransaction().rollback();
                }
                transactionManager.rollback(status);
                throw new RuntimeException("Error saving entity", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Transaction error", e);
        }
    }

    public <T> T get(Class<T> entityClass, Serializable id) {
        return execute(session -> session.get(entityClass, id));
    }

    public <T> List<T> find(String hql) {
        return execute(session -> {
            Query<T> query = session.createQuery(hql);
            return query.list();
        });
    }

    public <T> List<T> find(String hql, Object... params) {
        return execute(session -> {
            Query<T> query = session.createQuery(hql);
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
            return query.list();
        });
    }

    public void update(Object entity) {
        execute(session -> {
            session.update(entity);
            return null;
        });
    }

    public void delete(Object entity) {
        execute(session -> {
            session.delete(entity);
            return null;
        });
    }
} 