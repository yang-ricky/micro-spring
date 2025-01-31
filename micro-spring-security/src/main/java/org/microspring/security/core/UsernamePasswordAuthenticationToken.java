package org.microspring.security.core;

import java.util.Collection;
import java.util.Collections;

/**
 * 用户名密码认证令牌
 */
public class UsernamePasswordAuthenticationToken implements Authentication {
    private final Object principal;
    private Object credentials;
    private final Collection<? extends GrantedAuthority> authorities;
    private boolean authenticated;
    private Object details;

    /**
     * 创建一个未认证的令牌
     */
    public UsernamePasswordAuthenticationToken(Object principal, Object credentials) {
        this.principal = principal;
        this.credentials = credentials;
        this.authorities = Collections.emptyList();
        this.authenticated = false;
    }

    /**
     * 创建一个已认证的令牌
     */
    public UsernamePasswordAuthenticationToken(Object principal, Object credentials,
            Collection<? extends GrantedAuthority> authorities) {
        this.principal = principal;
        this.credentials = credentials;
        this.authorities = authorities;
        this.authenticated = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        if (authenticated && this.authorities.isEmpty()) {
            throw new IllegalArgumentException("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list");
        }
        this.authenticated = authenticated;
    }

    /**
     * 清除敏感信息
     */
    public void eraseCredentials() {
        this.credentials = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UsernamePasswordAuthenticationToken)) {
            return false;
        }
        UsernamePasswordAuthenticationToken other = (UsernamePasswordAuthenticationToken) obj;
        if (!principal.equals(other.principal)) {
            return false;
        }
        if (credentials == null && other.credentials != null) {
            return false;
        }
        if (credentials != null && !credentials.equals(other.credentials)) {
            return false;
        }
        return authorities.equals(other.authorities);
    }

    @Override
    public int hashCode() {
        int result = principal.hashCode();
        result = 31 * result + (credentials != null ? credentials.hashCode() : 0);
        result = 31 * result + authorities.hashCode();
        return result;
    }
} 