package org.microspring.orm;

import org.microspring.jdbc.transaction.JdbcTransactionManager;
import org.microspring.transaction.TransactionStatus;
import org.microspring.transaction.support.DefaultTransactionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

/**
 * ORM操作的模板类，封装HibernateTemplate并提供事务管理
 */
public class OrmTemplate {
    
    private static final Logger logger = LoggerFactory.getLogger(OrmTemplate.class);
    
    private final HibernateTemplate hibernateTemplate;
    private final JdbcTransactionManager transactionManager;

    public OrmTemplate(HibernateTemplate hibernateTemplate, JdbcTransactionManager transactionManager) {
        this.hibernateTemplate = hibernateTemplate;
        this.transactionManager = transactionManager;
    }

    public void save(Object entity) {
        TransactionStatus status = beginTransaction();
        try {
            hibernateTemplate.save(entity);
            commitTransaction(status);
            logger.debug("Successfully saved entity: {}", entity);
        } catch (Exception e) {
            rollbackTransaction(status);
            logger.error("Error saving entity: {}", e.getMessage());
            throw new RuntimeException("Failed to save entity", e);
        }
    }

    public <T> T get(Class<T> entityClass, Serializable id) {
        try {
            T entity = hibernateTemplate.get(entityClass, id);
            logger.debug("Found entity: {}", entity);
            return entity;
        } catch (Exception e) {
            logger.error("Error getting entity: {}", e.getMessage());
            throw new RuntimeException("Failed to get entity", e);
        }
    }

    public <T> List<T> find(String hql, Object... params) {
        try {
            List<T> results = hibernateTemplate.find(hql, params);
            logger.debug("Found {} entities", results.size());
            return results;
        } catch (Exception e) {
            logger.error("Error executing query: {}", e.getMessage());
            throw new RuntimeException("Failed to execute query", e);
        }
    }

    public void update(Object entity) {
        TransactionStatus status = beginTransaction();
        try {
            hibernateTemplate.update(entity);
            commitTransaction(status);
            logger.debug("Successfully updated entity: {}", entity);
        } catch (Exception e) {
            rollbackTransaction(status);
            logger.error("Error updating entity: {}", e.getMessage());
            throw new RuntimeException("Failed to update entity", e);
        }
    }

    public void delete(Object entity) {
        TransactionStatus status = beginTransaction();
        try {
            hibernateTemplate.delete(entity);
            commitTransaction(status);
            logger.debug("Successfully deleted entity: {}", entity);
        } catch (Exception e) {
            rollbackTransaction(status);
            logger.error("Error deleting entity: {}", e.getMessage());
            throw new RuntimeException("Failed to delete entity", e);
        }
    }

    private TransactionStatus beginTransaction() {
        try {
            return transactionManager.getTransaction(new DefaultTransactionDefinition());
        } catch (SQLException e) {
            logger.error("Error beginning transaction: {}", e.getMessage());
            throw new RuntimeException("Failed to begin transaction", e);
        }
    }

    private void commitTransaction(TransactionStatus status) {
        try {
            transactionManager.commit(status);
        } catch (SQLException e) {
            logger.error("Error committing transaction: {}", e.getMessage());
            throw new RuntimeException("Failed to commit transaction", e);
        }
    }

    private void rollbackTransaction(TransactionStatus status) {
        try {
            transactionManager.rollback(status);
        } catch (SQLException e) {
            logger.error("Error rolling back transaction: {}", e.getMessage());
            throw new RuntimeException("Failed to rollback transaction", e);
        }
    }
} 