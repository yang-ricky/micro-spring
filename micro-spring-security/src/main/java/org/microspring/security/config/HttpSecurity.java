package org.microspring.security.config;

import org.microspring.security.web.AuthorizationFilter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * HTTP安全配置类
 */
public class HttpSecurity {
    private final List<SecurityRule> rules = new ArrayList<>();
    private SecurityRule anyRequestRule;

    public AuthorizedUrl antMatchers(String... patterns) {
        return new AuthorizedUrl(patterns);
    }

    public AuthorizedUrl regexMatchers(String... patterns) {
        return new AuthorizedUrl(patterns, true);
    }

    public AuthorizedUrl anyRequest() {
        return new AuthorizedUrl(new String[] { "/**" }) {
            @Override
            void configure(SecurityRule rule) {
                anyRequestRule = rule;
            }
        };
    }

    public AuthorizationFilter build() {
        AuthorizationFilter filter = new AuthorizationFilter();
        
        // 添加所有规则
        for (SecurityRule rule : rules) {
            filter.addSecurityRule(rule);
        }
        
        // 添加默认规则
        if (anyRequestRule != null) {
            filter.setDefaultRule(anyRequestRule);
        }
        
        return filter;
    }

    public class AuthorizedUrl {
        private final String[] patterns;
        private final boolean isRegex;

        private AuthorizedUrl(String[] patterns) {
            this(patterns, false);
        }

        private AuthorizedUrl(String[] patterns, boolean isRegex) {
            this.patterns = patterns;
            this.isRegex = isRegex;
        }

        public HttpSecurity hasRole(String role) {
            SecurityRule rule = new SecurityRule(patterns, isRegex);
            rule.setRequiredRole("ROLE_" + role.toUpperCase());
            configure(rule);
            return HttpSecurity.this;
        }

        public HttpSecurity hasAnyRole(String... roles) {
            SecurityRule rule = new SecurityRule(patterns, isRegex);
            rule.setRequiredRoles(Arrays.stream(roles)
                .map(role -> "ROLE_" + role.toUpperCase())
                .toArray(String[]::new));
            configure(rule);
            return HttpSecurity.this;
        }

        public HttpSecurity hasAuthority(String authority) {
            SecurityRule rule = new SecurityRule(patterns, isRegex);
            rule.setRequiredAuthority(authority);
            configure(rule);
            return HttpSecurity.this;
        }

        public HttpSecurity hasIpAddress(String ipPattern) {
            SecurityRule rule = new SecurityRule(patterns, isRegex);
            rule.setRequiredIpPattern(ipPattern);
            configure(rule);
            return HttpSecurity.this;
        }

        public HttpSecurity authenticated() {
            SecurityRule rule = new SecurityRule(patterns, isRegex);
            rule.setRequireAuthenticated(true);
            configure(rule);
            return HttpSecurity.this;
        }

        public HttpSecurity permitAll() {
            SecurityRule rule = new SecurityRule(patterns, isRegex);
            rule.setPermitAll(true);
            configure(rule);
            return HttpSecurity.this;
        }

        void configure(SecurityRule rule) {
            rules.add(rule);
        }
    }
} 