package org.microspring.security.core;

/**
 * 表示授予的权限
 */
public interface GrantedAuthority {
    /**
     * 获取权限字符串表示
     * 例如：ROLE_ADMIN, ROLE_USER
     */
    String getAuthority();
} 