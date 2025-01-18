package org.microspring.jdbc.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

public class ConnectionProxy implements InvocationHandler {
    private final Connection target;
    private final ConnectionPool pool;
    
    private ConnectionProxy(Connection target, ConnectionPool pool) {
        this.target = target;
        this.pool = pool;
    }
    
    public static Connection newProxy(Connection target, ConnectionPool pool) {
        return (Connection) Proxy.newProxyInstance(
            ConnectionProxy.class.getClassLoader(),
            new Class<?>[] { Connection.class },
            new ConnectionProxy(target, pool)
        );
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 特殊处理 close 方法
        if ("close".equals(method.getName())) {
            pool.releaseConnection(target);
            return null;
        }
        // 其他方法直接委托给目标连接
        return method.invoke(target, args);
    }
} 