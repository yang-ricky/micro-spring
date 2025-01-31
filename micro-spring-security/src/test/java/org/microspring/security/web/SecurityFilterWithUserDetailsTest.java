package org.microspring.security.web;

import org.apache.commons.codec.binary.Base64;
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

public class SecurityFilterWithUserDetailsTest {
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

    @Test
    public void testValidAuthentication() throws Exception {
        // 准备Basic认证头
        String username = "testUser";
        String password = "testPass";
        String credentials = username + ":" + password;
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull("Authentication should not be null", auth);
        assertTrue("Authentication should be authenticated", auth.isAuthenticated());
        assertEquals("Principal should match username", username, auth.getPrincipal());
    }

    @Test
    public void testInvalidPassword() throws Exception {
        String username = "testUser";
        String wrongPassword = "wrongPass";
        String credentials = username + ":" + wrongPassword;
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), contains("Invalid password"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testNonExistentUser() throws Exception {
        String username = "nonExistentUser";
        String password = "testPass";
        String credentials = username + ":" + password;
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), contains("User not found"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testDisabledUser() throws Exception {
        // 创建一个禁用的用户
        String username = "disabledUser";
        String password = "testPass";
        String encodedPassword = passwordEncoder.encode(password);
        UserDetails disabledUser = User.builder()
            .username(username)
            .password(encodedPassword)
            .enabled(false)
            .build();
        userDetailsService.createUser(disabledUser);

        String credentials = username + ":" + password;
        String base64Credentials = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
        
        when(request.getHeader("Authorization")).thenReturn("Basic " + base64Credentials);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Micro Spring Security\"");
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), contains("disabled"));
        verify(filterChain, never()).doFilter(request, response);
    }
} 