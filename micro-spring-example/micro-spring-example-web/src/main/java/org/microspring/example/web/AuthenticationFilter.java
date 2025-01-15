package org.microspring.example.web;

import javax.servlet.*;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

public class AuthenticationFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化逻辑
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
            FilterChain chain) throws IOException, ServletException {
        // 在这里实现认证逻辑
        boolean isAuthenticated = checkAuthentication(request);
        
        if (isAuthenticated) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 设置401状态码
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Authentication failed\"}");
        }
    }

    @Override
    public void destroy() {
        // 清理资源
    }

    private boolean checkAuthentication(ServletRequest request) {
        // 实现你的认证逻辑
        return true;
    }
}