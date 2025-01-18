package org.microspring.orm;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.hibernate.Transaction;

public class HibernateTemplate {
    
    private static final Logger logger = LoggerFactory.getLogger(HibernateTemplate.class);
    private final SessionFactory sessionFactory;

    public HibernateTemplate(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public <T> T execute(HibernateCallback<T> action) {
        Session session = sessionFactory.getCurrentSession();
        try {
            return action.doInHibernate(session);
        } catch (Exception e) {
            logger.error("Error executing Hibernate action", e);
            throw new RuntimeException("Error executing Hibernate action", e);
        }
    }

    public void save(Object entity) {
        execute(session -> {
            session.save(entity);
            return null;
        });
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