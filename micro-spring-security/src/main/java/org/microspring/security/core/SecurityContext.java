package org.microspring.security.core;

/**
 * 存储认证信息的上下文接口
 */
public interface SecurityContext {
    /**
     * 获取当前认证信息
     */
    Authentication getAuthentication();

    /**
     * 设置认证信息
     */
    void setAuthentication(Authentication authentication);
} 