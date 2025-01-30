package org.microspring.security.web;

import org.apache.commons.codec.binary.Base64;
import org.microspring.security.core.SecurityContext;
import org.microspring.security.core.SecurityContextHolder;
import org.microspring.security.core.UsernamePasswordAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 安全过滤器，实现Basic认证
 */
public class SecurityFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";

    @Override
    public void init(FilterConfig filterConfig) {
        // 初始化时不需要特殊处理
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 获取Authorization头
            String authHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);
            
            if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX)) {
                unauthorized(httpResponse, "Missing or invalid Authorization header");
                return;
            }

            // 解析Basic认证信息
            String base64Credentials = authHeader.substring(BASIC_PREFIX.length()).trim();
            if (base64Credentials.isEmpty()) {
                unauthorized(httpResponse, "Empty credentials");
                return;
            }

            String credentials;
            try {
                credentials = new String(Base64.decodeBase64(base64Credentials), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                unauthorized(httpResponse, "Invalid Base64 encoding");
                return;
            }

            // 检查是否只包含一个冒号
            if (credentials.indexOf(':') != credentials.lastIndexOf(':')) {
                unauthorized(httpResponse, "Invalid Basic authentication format: multiple colons found");
                return;
            }

            String[] values = credentials.split(":", 2);
            
            if (values.length != 2) {
                unauthorized(httpResponse, "Invalid Basic authentication format");
                return;
            }

            String username = values[0];
            String password = values[1];

            // 验证用户名和密码不能为空
            if (username.trim().isEmpty() || password.trim().isEmpty()) {
                unauthorized(httpResponse, "Username and password cannot be empty");
                return;
            }

            // 创建认证token并存储到SecurityContext
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
            
            // TODO: 这里应该进行实际的用户名密码验证
            // 现在为了演示，我们简单地认为所有请求都是认证成功的
            token.setAuthenticated(true);
            
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(token);

            // 继续处理请求
            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Authentication error", e);
            unauthorized(httpResponse, "Internal authentication error");
        }
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        logger.warn("Unauthorized access attempt: {}", message);
        response.setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    @Override
    public void destroy() {
        // 在过滤器销毁时清理SecurityContext
        SecurityContextHolder.clearContext();
    }
} 