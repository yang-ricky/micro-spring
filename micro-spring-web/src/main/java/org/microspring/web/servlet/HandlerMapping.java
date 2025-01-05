package org.microspring.web.servlet;

import javax.servlet.http.HttpServletRequest;
import org.microspring.web.method.HandlerMethod;

public interface HandlerMapping {
    HandlerMethod getHandler(HttpServletRequest request);
} 