package org.microspring.security.core;

/**
 * 使用ThreadLocal存储SecurityContext的工具类
 */
public class SecurityContextHolder {
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

    public static void clearContext() {
        contextHolder.remove();
    }

    public static SecurityContext getContext() {
        SecurityContext ctx = contextHolder.get();
        if (ctx == null) {
            ctx = createEmptyContext();
            contextHolder.set(ctx);
        }
        return ctx;
    }

    public static void setContext(SecurityContext context) {
        if (context == null) {
            throw new IllegalArgumentException("SecurityContext cannot be null");
        }
        contextHolder.set(context);
    }

    private static SecurityContext createEmptyContext() {
        return new SecurityContextImpl();
    }
} 