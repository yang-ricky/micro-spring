package org.microspring.security.core;

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