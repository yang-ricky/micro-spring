package org.microspring.security.web;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.microspring.security.core.Authentication;
import org.microspring.security.core.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SecurityFilterTest {
    private SecurityFilter securityFilter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @Before
    public void setUp() {
        securityFilter = new SecurityFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        // 确保每个测试开始时SecurityContext是干净的
        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
        // 确保每个测试结束后清理SecurityContext
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testValidBasicAuth() throws Exception {
        // 准备Basic认证头
        String username = "testUser";
        String password = "testPass";
        String credentials = username + ":" + password;
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        // 执行过滤器
        securityFilter.doFilter(request, response, filterChain);

        // 验证filterChain被调用（请求被允许继续）
        verify(filterChain).doFilter(request, response);
        
        // 验证SecurityContext中的认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull("Authentication should not be null", auth);
        assertTrue("Authentication should be authenticated", auth.isAuthenticated());
        assertEquals("Principal should match username", username, auth.getPrincipal());
        assertEquals("Credentials should match password", password, auth.getCredentials());
    }

    @Test
    public void testMissingAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testInvalidAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Invalid");

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testEmptyBasicAuth() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic ");

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testInvalidBase64Credentials() throws Exception {
        // 提供一个无效的Base64字符串
        when(request.getHeader("Authorization")).thenReturn("Basic !@#$%^&*");

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testMissingPassword() throws Exception {
        // 只有用户名，没有密码
        String credentials = "testUser:";
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testMissingUsername() throws Exception {
        // 只有密码，没有用户名
        String credentials = ":testPass";
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testEmptyCredentials() throws Exception {
        // 用户名和密码都为空
        String credentials = ":";
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testMultipleColonsInCredentials() throws Exception {
        // 凭证中包含多个冒号
        String credentials = "testUser:test:Pass";
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testSpecialCharactersInCredentials() throws Exception {
        // 用户名和密码中包含特殊字符
        String username = "test@user.com";
        String password = "pass#word!123";
        String credentials = username + ":" + password;
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull("Authentication should not be null", auth);
        assertTrue("Authentication should be authenticated", auth.isAuthenticated());
        assertEquals("Principal should match username with special characters", username, auth.getPrincipal());
        assertEquals("Credentials should match password with special characters", password, auth.getCredentials());
    }
}