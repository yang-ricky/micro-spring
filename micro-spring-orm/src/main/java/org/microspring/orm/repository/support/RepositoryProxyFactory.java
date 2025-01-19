package org.microspring.orm.repository.support;

import org.microspring.orm.OrmTemplate;
import org.microspring.orm.repository.CrudRepository;
import org.hibernate.Session;
import org.microspring.orm.transaction.TransactionCallback;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.Optional;

public class RepositoryProxyFactory {
    
    @SuppressWarnings("unchecked")
    public static <T, ID extends Serializable> CrudRepository<T, ID> createRepository(
            Class<?> repositoryInterface, OrmTemplate ormTemplate) {
        // 获取实体类型
        Class<T> entityClass = (Class<T>) ((ParameterizedType) repositoryInterface.getGenericInterfaces()[0])
                .getActualTypeArguments()[0];
        
        return (CrudRepository<T, ID>) Proxy.newProxyInstance(
            repositoryInterface.getClassLoader(),
            new Class[]{repositoryInterface},
            new RepositoryInvocationHandler<>(entityClass, ormTemplate)
        );
    }
    
    private static class RepositoryInvocationHandler<T, ID extends Serializable> implements InvocationHandler {
        private final Class<T> entityClass;
        private final OrmTemplate ormTemplate;
        
        public RepositoryInvocationHandler(Class<T> entityClass, OrmTemplate ormTemplate) {
            this.entityClass = entityClass;
            this.ormTemplate = ormTemplate;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            
            switch (methodName) {
                case "save":
                    return doInTransaction(session -> {
                        session.saveOrUpdate(args[0]);
                        return args[0];
                    });
                    
                case "findById":
                    return Optional.ofNullable(doInTransaction(session -> 
                        session.get(entityClass, (Serializable) args[0])));
                    
                case "existsById":
                    return doInTransaction(session -> 
                        session.get(entityClass, (Serializable) args[0]) != null);
                    
                case "findAll":
                    return doInTransaction(session -> 
                        session.createQuery("from " + entityClass.getName(), entityClass).list());
                    
                case "count":
                    return doInTransaction(session -> {
                        Number count = (Number) session.createQuery("select count(*) from " + entityClass.getName())
                            .uniqueResult();
                        return count.longValue();
                    });
                    
                case "deleteById":
                    return doInTransaction(session -> {
                        Object entity = session.get(entityClass, (Serializable) args[0]);
                        if (entity != null) {
                            session.delete(entity);
                        }
                        return null;
                    });
                    
                case "delete":
                    return doInTransaction(session -> {
                        session.delete(args[0]);
                        return null;
                    });
                    
                case "deleteAll":
                    return doInTransaction(session -> {
                        session.createQuery("delete from " + entityClass.getName()).executeUpdate();
                        return null;
                    });
                    
                default:
                    // 检查是否是查询方法
                    if (QueryMethodParser.isQueryMethod(method)) {
                        return doInTransaction(session -> {
                            QueryMethodParser.QueryMethod queryMethod = 
                                QueryMethodParser.parseMethod(method, entityClass);
                            
                            org.hibernate.query.Query<T> query = session.createQuery(queryMethod.getQueryString(), entityClass);
                            // 设置多个参数
                            for (int i = 0; i < args.length; i++) {
                                query.setParameter(i + 1, args[i]);
                            }
                            return query.getResultList();
                        });
                    }
                    throw new UnsupportedOperationException("Method not implemented: " + methodName);
            }
        }
        
        private <R> R doInTransaction(SessionCallback<R> callback) {
            return ormTemplate.executeInTransaction((template, session) -> {
                try {
                    return callback.doInSession(session);
                } catch (Exception e) {
                    throw new RuntimeException("Transaction failed", e);
                }
            });
        }
    }
} 