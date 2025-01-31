package org.microspring.security.core.context;

import org.microspring.security.core.Authentication;

/**
 * 安全上下文接口
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