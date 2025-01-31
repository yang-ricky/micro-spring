package org.microspring.security.core;

import java.util.Objects;

/**
 * GrantedAuthority的简单实现
 */
public class SimpleGrantedAuthority implements GrantedAuthority {
    private final String authority;

    public SimpleGrantedAuthority(String authority) {
        if (authority == null || authority.trim().isEmpty()) {
            throw new IllegalArgumentException("Authority cannot be null or empty");
        }
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleGrantedAuthority that = (SimpleGrantedAuthority) o;
        return Objects.equals(authority, that.authority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authority);
    }

    @Override
    public String toString() {
        return authority;
    }
} 