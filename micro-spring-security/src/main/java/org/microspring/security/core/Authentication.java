package org.microspring.security.core;

import java.util.Collection;
import java.util.Collections;

/**
 * 认证信息接口
 */
public interface Authentication {
    /**
     * 获取权限列表
     */
    Collection<? extends GrantedAuthority> getAuthorities();

    /**
     * 获取凭证（通常是密码）
     */
    Object getCredentials();

    /**
     * 获取详细信息
     */
    Object getDetails();

    /**
     * 获取主体（通常是用户名）
     */
    Object getPrincipal();

    /**
     * 是否已认证
     */
    boolean isAuthenticated();

    /**
     * 设置认证状态
     */
    void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException;

    /**
     * 创建一个未认证的Authentication对象
     */
    static Authentication unauthenticated() {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return Collections.emptyList();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return "anonymousUser";
            }

            @Override
            public boolean isAuthenticated() {
                return false;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
                throw new IllegalArgumentException("Cannot change authentication status");
            }
        };
    }
} 