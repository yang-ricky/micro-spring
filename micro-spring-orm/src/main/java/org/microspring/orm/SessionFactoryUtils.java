package org.microspring.orm;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.util.logging.Logger;
import java.util.logging.Level;

public class SessionFactoryUtils {
    
    private static final Logger logger = Logger.getLogger(SessionFactoryUtils.class.getName());
    
    public static Session getSession(SessionFactory sessionFactory) {
        if (sessionFactory == null) {
            throw new IllegalStateException("No SessionFactory configured");
        }
        
        try {
            return sessionFactory.openSession();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not open Hibernate Session", e);
            throw new RuntimeException("Could not open Hibernate Session", e);
        }
    }
    
    public static void closeSession(Session session) {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not close Hibernate Session", e);
            }
        }
    }
} 