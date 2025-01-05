package org.microspring.web.http.converter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public class StringHttpMessageConverter implements HttpMessageConverter<String> {
    
    @Override
    public boolean canRead(Class<?> clazz, String mediaType) {
        return String.class.isAssignableFrom(clazz);
    }
    
    @Override
    public boolean canWrite(Class<?> clazz, String mediaType) {
        return String.class.isAssignableFrom(clazz);
    }
    
    @Override
    public String read(Class<? extends String> clazz, HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
    
    @Override
    public void write(String str, String contentType, HttpServletResponse response) throws IOException {
        response.setContentType(contentType);
        response.getWriter().write(str);
    }
} 