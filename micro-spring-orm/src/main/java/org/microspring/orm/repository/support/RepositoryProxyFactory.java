package org.microspring.orm.repository.support;

import org.microspring.orm.OrmTemplate;
import org.microspring.orm.repository.CrudRepository;
import org.hibernate.Session;
import org.microspring.orm.transaction.TransactionCallback;
import org.microspring.orm.repository.Sort;
import org.microspring.orm.repository.Pageable;
import org.microspring.orm.repository.support.QueryMethodParser.QueryMethod;
import org.microspring.orm.repository.Query;

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
                    if (QueryMethodParser.isQueryMethod(method) || method.isAnnotationPresent(Query.class)) {
                        return executeQuery(method, args);
                    }
                    throw new UnsupportedOperationException("Method not implemented: " + methodName);
            }
        }
        
        private Object executeQuery(Method method, Object[] args) {
            QueryMethod queryMethod = QueryMethodParser.parseMethod(method, entityClass);
            final String finalQueryString;
            final Pageable finalPageable;
            
            // 处理排序和分页
            if (queryMethod.isPageable() && args != null && args.length > 0) {
                Pageable pageable = (Pageable) args[args.length - 1];
                if (pageable != null) {
                    String queryString = queryMethod.getQueryString();
                    
                    // 添加排序
                    if (pageable.getSort() != null) {
                        StringBuilder orderBy = new StringBuilder(" order by ");
                        for (Sort.Order order : pageable.getSort().getOrders()) {
                            orderBy.append(order.getProperty())
                                   .append(" ")
                                   .append(order.getDirection().name())
                                   .append(",");
                        }
                        orderBy.setLength(orderBy.length() - 1); // 移除最后的逗号
                        queryString = queryString.replace("#{orderBy}", orderBy.toString());
                    } else {
                        queryString = queryString.replace("#{orderBy}", "");
                    }
                    
                    finalQueryString = queryString;
                    finalPageable = pageable;
                } else {
                    finalQueryString = queryMethod.getQueryString();
                    finalPageable = null;
                }
            } else {
                // 处理只有Sort参数的情况
                Sort sort = null;
                if (args != null && args.length > 0 && args[args.length - 1] instanceof Sort) {
                    sort = (Sort) args[args.length - 1];
                }
                
                String queryString = queryMethod.getQueryString();
                if (sort != null) {
                    StringBuilder orderBy = new StringBuilder(" order by ");
                    for (Sort.Order order : sort.getOrders()) {
                        orderBy.append(order.getProperty())
                               .append(" ")
                               .append(order.getDirection().name())
                               .append(",");
                    }
                    orderBy.setLength(orderBy.length() - 1); // 移除最后的逗号
                    queryString += orderBy.toString();
                }
                
                finalQueryString = queryString;
                finalPageable = null;
            }
            
            return doInTransaction(session -> {
                org.hibernate.query.Query<T> query = session.createQuery(finalQueryString, entityClass);
                
                // 设置分页参数
                if (finalPageable != null) {
                    query.setFirstResult(finalPageable.getPageNumber() * finalPageable.getPageSize())
                         .setMaxResults(finalPageable.getPageSize());
                }
                
                // 设置查询参数
                if (args != null) {
                    int paramIndex = 1;
                    for (Object arg : args) {
                        if (!(arg instanceof Pageable) && !(arg instanceof Sort)) {
                            query.setParameter(paramIndex++, arg);
                        }
                    }
                }
                
                return query.getResultList();
            });
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