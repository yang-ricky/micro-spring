package org.microspring.security.web;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.microspring.security.core.*;
import org.microspring.security.crypto.password.BCryptPasswordEncoder;
import org.microspring.security.crypto.password.PasswordEncoder;

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
    private InMemoryUserDetailsService userDetailsService;
    private PasswordEncoder passwordEncoder;

    @Before
    public void setUp() {
        userDetailsService = new InMemoryUserDetailsService();
        passwordEncoder = new BCryptPasswordEncoder();
        securityFilter = new SecurityFilter(userDetailsService, passwordEncoder);
        
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        // 创建一个测试用户
        String rawPassword = "testPass";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        UserDetails user = User.builder()
            .username("testUser")
            .password(encodedPassword)
            .build();
        userDetailsService.createUser(user);

        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
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
        
        // 创建带特殊字符的用户
        String encodedPassword = passwordEncoder.encode(password);
        UserDetails specialUser = User.builder()
            .username(username)
            .password(encodedPassword)
            .build();
        userDetailsService.createUser(specialUser);

        String credentials = username + ":" + password;
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull("Authentication should not be null", auth);
        assertTrue("Authentication should be authenticated", auth.isAuthenticated());
        assertEquals("Principal should match username with special characters", username, auth.getPrincipal());
    }
}