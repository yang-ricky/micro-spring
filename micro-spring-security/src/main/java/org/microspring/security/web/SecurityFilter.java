package org.microspring.security.web;

import org.apache.commons.codec.binary.Base64;
import org.microspring.security.core.*;
import org.microspring.security.crypto.password.PasswordEncoder;
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

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SecurityFilter(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

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

            String[] values = credentials.split(":", 2);
            
            if (values.length != 2) {
                unauthorized(httpResponse, "Invalid Basic authentication format");
                return;
            }

            String username = values[0];
            String password = values[1];

            try {
                // 加载用户信息
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 验证用户状态
                if (!userDetails.isEnabled()) {
                    unauthorized(httpResponse, "User account is disabled");
                    return;
                }
                if (!userDetails.isAccountNonLocked()) {
                    unauthorized(httpResponse, "User account is locked");
                    return;
                }
                if (!userDetails.isAccountNonExpired()) {
                    unauthorized(httpResponse, "User account is expired");
                    return;
                }
                if (!userDetails.isCredentialsNonExpired()) {
                    unauthorized(httpResponse, "User credentials are expired");
                    return;
                }

                // 验证密码
                if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                    unauthorized(httpResponse, "Invalid password");
                    return;
                }

                // 创建认证token并存储到SecurityContext
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    username, null, userDetails.getAuthorities());
                
                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(token);

                chain.doFilter(request, response);
            } catch (UsernameNotFoundException e) {
                unauthorized(httpResponse, "User not found: " + username);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            unauthorized(httpResponse, "Internal authentication error");
            return;
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