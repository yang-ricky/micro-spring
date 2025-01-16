package org.microspring.context.scope;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.microspring.beans.factory.annotation.Scope;

public class ScopeManager {
    private static final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();
    private static final ThreadLocal<HttpSession> currentSession = new ThreadLocal<>();
    
    // 存储 request 作用域的 bean
    private final Map<String, Map<String, Object>> requestScoped = new ConcurrentHashMap<>();
    
    // 存储 session 作用域的 bean
    private final Map<String, Map<String, Object>> sessionScoped = new ConcurrentHashMap<>();
    
    public void setCurrentRequest(HttpServletRequest request) {
        currentRequest.set(request);
    }
    
    public void setCurrentSession(HttpSession session) {
        currentSession.set(session);
    }
    
    public Object getBean(String name, String scope, ObjectFactory factory) {
        if (Scope.REQUEST.equals(scope)) {
            return getRequestScopedBean(name, factory);
        }
        if (Scope.SESSION.equals(scope)) {
            return getSessionScopedBean(name, factory);
        }
        throw new IllegalArgumentException("Unknown scope: " + scope);
    }
    
    private Object getRequestScopedBean(String name, ObjectFactory factory) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            throw new IllegalStateException("No request bound to current thread");
        }
        
        String requestId = request.getRequestedSessionId();
        Map<String, Object> requestBeans = requestScoped.computeIfAbsent(requestId, k -> new ConcurrentHashMap<>());
        
        // Request 作用域：每个请求都创建新实例
        return requestBeans.computeIfAbsent(name, k -> factory.getObject());
    }
    
    private Object getSessionScopedBean(String name, ObjectFactory factory) {
        HttpSession session = getCurrentSession();
        if (session == null) {
            throw new IllegalStateException("No session bound to current thread");
        }
        
        String sessionId = session.getId();
        Map<String, Object> sessionBeans = sessionScoped.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
        
        // Session 作用域：同一会话复用实例
        return sessionBeans.computeIfAbsent(name, k -> factory.getObject());
    }
    
    // 清理过期的 request 作用域 bean
    public void cleanupRequestScope(String requestId) {
        requestScoped.remove(requestId);
    }
    
    // 清理过期的 session 作用域 bean
    public void cleanupSessionScope(String sessionId) {
        sessionScoped.remove(sessionId);
    }
    
    public HttpServletRequest getCurrentRequest() {
        return currentRequest.get();
    }
    
    public HttpSession getCurrentSession() {
        return currentSession.get();
    }
} 