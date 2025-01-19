package org.microspring.orm;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

import org.microspring.orm.transaction.TransactionCallback;

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

    public enum PropagationBehavior {
        REQUIRED,      // 如果当前没有事务，就新建一个事务；如果已经存在事务，就加入到这个事务中
        REQUIRES_NEW,  // 无论是否存在事务，都创建新事务
        SUPPORTS      // 如果当前存在事务，就加入该事务；如果当前没有事务，就以非事务执行
    }

    public <T> T executeInTransaction(TransactionCallback<T> action, PropagationBehavior propagation) {
        return executeInTransaction(action, propagation, java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }

    public <T> T executeInTransaction(TransactionCallback<T> action, int isolationLevel) {
        return executeInTransaction(action, PropagationBehavior.REQUIRED, isolationLevel);
    }

    public <T> T executeInTransaction(TransactionCallback<T> action) {
        return executeInTransaction(action, PropagationBehavior.REQUIRED, java.sql.Connection.TRANSACTION_READ_COMMITTED);
    }

    public <T> T executeInTransaction(TransactionCallback<T> action, PropagationBehavior propagation, int isolationLevel) {
        Session session = sessionFactory.getCurrentSession();
        Transaction currentTx = session.getTransaction();
        boolean isExistingTransaction = currentTx != null && currentTx.isActive();

        switch (propagation) {
            case REQUIRES_NEW:
                if (isExistingTransaction) {
                    logger.debug("Suspending current transaction for REQUIRES_NEW");
                    session.flush();
                    
                    Session newSession = sessionFactory.openSession();
                    try {
                        logger.debug("Starting new transaction for REQUIRES_NEW");
                        T result = doInNewTransaction(action, newSession, isolationLevel);
                        logger.debug("New transaction completed successfully");
                        return result;
                    } finally {
                        logger.debug("Resuming original transaction");
                        newSession.close();
                    }
                }
                return doInNewTransaction(action, session, isolationLevel);

            case SUPPORTS:
                if (isExistingTransaction) {
                    return action.doInTransaction(this, session);
                }
                return doInNewTransaction(action, session, isolationLevel);

            case REQUIRED:
            default:
                if (!isExistingTransaction) {
                    return doInNewTransaction(action, session, isolationLevel);
                }
                return action.doInTransaction(this, session);
        }
    }

    private <T> T doInNewTransaction(TransactionCallback<T> action, Session session, int isolationLevel) {
        Transaction tx = session.beginTransaction();
        try {
            logger.debug("Setting transaction isolation level: {}", isolationLevel);
            session.doWork(connection -> connection.setTransactionIsolation(isolationLevel));
            
            logger.debug("Executing transaction callback");
            T result = action.doInTransaction(this, session);
            
            logger.debug("Committing transaction");
            tx.commit();
            return result;
        } catch (Exception e) {
            logger.error("Transaction failed", e);
            if (tx != null && tx.isActive()) {
                logger.debug("Rolling back transaction");
                tx.rollback();
            }
            throw new RuntimeException("Transaction failed", e);
        }
    }

    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }
} 