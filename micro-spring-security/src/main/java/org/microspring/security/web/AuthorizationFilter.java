package org.microspring.security.web;

import org.microspring.security.config.SecurityRule;
import org.microspring.security.core.Authentication;
import org.microspring.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 授权过滤器，实现基于角色的访问控制
 */
public class AuthorizationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);
    private final List<SecurityRule> securityRules = new ArrayList<>();
    private SecurityRule defaultRule;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI();
        String remoteAddr = request.getRemoteAddr();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            authentication = Authentication.unauthenticated();
        }

        SecurityRule matchingRule = findMatchingRule(path);
        if (matchingRule == null) {
            if (defaultRule != null) {
                matchingRule = defaultRule;
            } else {
                // 如果没有匹配的规则且没有默认规则，拒绝访问
                logger.warn("Access denied for path {} - no matching rule found", path);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
        }

        if (!matchingRule.checkAccess(authentication, remoteAddr)) {
            logger.warn("Access denied for path {} - failed security check", path);
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    public void addSecurityRule(SecurityRule rule) {
        securityRules.add(rule);
    }

    public void setDefaultRule(SecurityRule rule) {
        this.defaultRule = rule;
    }

    private SecurityRule findMatchingRule(String path) {
        // 按添加顺序匹配规则，返回第一个匹配的
        return securityRules.stream()
            .filter(rule -> rule.matches(path))
            .findFirst()
            .orElse(null);
    }
} 