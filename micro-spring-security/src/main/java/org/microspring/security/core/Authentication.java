package org.microspring.security.core;

/**
 * 认证信息接口
 */
public interface Authentication {
    /**
     * 获取认证主体（通常是用户名）
     */
    String getPrincipal();

    /**
     * 获取凭证（通常是密码）
     */
    String getCredentials();

    /**
     * 是否已认证
     */
    boolean isAuthenticated();

    /**
     * 设置认证状态
     */
    void setAuthenticated(boolean isAuthenticated);
} 