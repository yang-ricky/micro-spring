package org.microspring.security.core.context;

import org.microspring.security.core.Authentication;

/**
 * 安全上下文持有者，使用ThreadLocal存储认证信息
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
            throw new IllegalArgumentException("Only non-null SecurityContext instances are permitted");
        }
        contextHolder.set(context);
    }

    private static SecurityContext createEmptyContext() {
        return new SecurityContextImpl();
    }
} 