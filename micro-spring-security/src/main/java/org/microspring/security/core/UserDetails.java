package org.microspring.security.core;

import java.util.Collection;

/**
 * 用户详细信息接口
 */
public interface UserDetails {
    /**
     * 获取用户名
     */
    String getUsername();

    /**
     * 获取密码
     */
    String getPassword();

    /**
     * 获取用户的权限列表
     */
    Collection<? extends GrantedAuthority> getAuthorities();

    /**
     * 账户是否未过期
     */
    boolean isAccountNonExpired();

    /**
     * 账户是否未锁定
     */
    boolean isAccountNonLocked();

    /**
     * 凭证是否未过期
     */
    boolean isCredentialsNonExpired();

    /**
     * 账户是否启用
     */
    boolean isEnabled();
} 