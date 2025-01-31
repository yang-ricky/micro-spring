package org.microspring.security.core.context;

import org.microspring.security.core.Authentication;

/**
 * 安全上下文实现类
 */
public class SecurityContextImpl implements SecurityContext {
    private Authentication authentication;

    @Override
    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SecurityContextImpl)) {
            return false;
        }
        SecurityContextImpl other = (SecurityContextImpl) obj;
        if (this.getAuthentication() == null && other.getAuthentication() == null) {
            return true;
        }
        if (this.getAuthentication() != null && other.getAuthentication() != null) {
            return this.getAuthentication().equals(other.getAuthentication());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return authentication == null ? -1 : authentication.hashCode();
    }
} 