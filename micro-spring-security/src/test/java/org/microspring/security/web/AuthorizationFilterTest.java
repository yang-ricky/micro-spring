package org.microspring.security.web;

import org.junit.Before;
import org.junit.Test;
import org.microspring.security.config.HttpSecurity;
import org.microspring.security.core.Authentication;
import org.microspring.security.core.GrantedAuthority;
import org.microspring.security.core.SimpleGrantedAuthority;
import org.microspring.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

public class AuthorizationFilterTest {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private Authentication authentication;
    private AuthorizationFilter filter;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        authentication = mock(Authentication.class);
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void configureAuthentication(String... authorities) {
        when(authentication.isAuthenticated()).thenReturn(true);
        Collection authorities2 = Arrays.stream(authorities)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
        when(authentication.getAuthorities()).thenReturn(authorities2);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void testPublicPath() throws Exception {
        // 配置安全规则
        HttpSecurity http = new HttpSecurity();
        filter = http
            .antMatchers("/public/**").permitAll()
            .build();

        // 测试公开路径
        when(request.getRequestURI()).thenReturn("/public/index.html");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    public void testAdminPathWithCorrectRole() throws Exception {
        // 配置安全规则
        HttpSecurity http = new HttpSecurity();
        filter = http
            .antMatchers("/admin/**").hasRole("ADMIN")
            .build();

        // 配置管理员角色
        configureAuthentication("ROLE_ADMIN");

        // 测试管理路径
        when(request.getRequestURI()).thenReturn("/admin/dashboard");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    public void testAdminPathWithWrongRole() throws Exception {
        // 配置安全规则
        HttpSecurity http = new HttpSecurity();
        filter = http
            .antMatchers("/admin/**").hasRole("ADMIN")
            .build();

        // 配置普通用户角色
        configureAuthentication("ROLE_USER");

        // 测试管理路径
        when(request.getRequestURI()).thenReturn("/admin/dashboard");
        filter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    public void testRegexPattern() throws Exception {
        // 配置安全规则
        HttpSecurity http = new HttpSecurity();
        filter = http
            .regexMatchers("/api/v\\d+/.*").hasRole("API_USER")
            .build();

        // 配置API用户角色
        configureAuthentication("ROLE_API_USER");

        // 测试API路径
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    public void testMultipleRoles() throws Exception {
        // 配置安全规则
        HttpSecurity http = new HttpSecurity();
        filter = http
            .antMatchers("/management/**").hasAnyRole("ADMIN", "MANAGER")
            .build();

        // 配置管理者角色
        configureAuthentication("ROLE_MANAGER");

        // 测试管理路径
        when(request.getRequestURI()).thenReturn("/management/reports");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    public void testCustomAuthority() throws Exception {
        // 配置安全规则
        HttpSecurity http = new HttpSecurity();
        filter = http
            .antMatchers("/documents/**").hasAuthority("documents:write")
            .build();

        // 配置自定义权限
        configureAuthentication("documents:write");

        // 测试文档路径
        when(request.getRequestURI()).thenReturn("/documents/report.pdf");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    public void testIpAddressRestriction() throws Exception {
        // 配置安全规则
        HttpSecurity http = new HttpSecurity();
        filter = http
            .antMatchers("/internal/**").hasIpAddress("192.168.1.0/24")
            .build();

        // 配置IP地址
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // 测试内部路径
        when(request.getRequestURI()).thenReturn("/internal/status");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    public void testRulePriority() throws Exception {
        // 配置安全规则
        HttpSecurity http = new HttpSecurity();
        filter = http
            .antMatchers("/api/admin/**").hasRole("ADMIN")
            .antMatchers("/api/**").hasRole("USER")
            .build();

        // 配置管理员角色
        configureAuthentication("ROLE_ADMIN");

        // 测试特定管理路径
        when(request.getRequestURI()).thenReturn("/api/admin/users");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    public void testDefaultRule() throws Exception {
        // 配置安全规则
        HttpSecurity http = new HttpSecurity();
        filter = http
            .antMatchers("/public/**").permitAll()
            .anyRequest().authenticated()
            .build();

        // 未认证用户访问非公开路径
        when(request.getRequestURI()).thenReturn("/private/data");
        filter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verify(chain, never()).doFilter(request, response);

        // 认证用户访问非公开路径
        configureAuthentication("ROLE_USER");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
} 