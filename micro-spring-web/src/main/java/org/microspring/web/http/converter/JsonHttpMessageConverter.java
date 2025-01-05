package org.microspring.web.http.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JsonHttpMessageConverter implements HttpMessageConverter<Object> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean canRead(Class<?> clazz, String mediaType) {
        return mediaType != null && mediaType.contains("application/json");
    }
    
    @Override
    public boolean canWrite(Class<?> clazz, String mediaType) {
        return mediaType != null && mediaType.contains("application/json");
    }
    
    @Override
    public Object read(Class<?> clazz, HttpServletRequest request) throws IOException {
        return objectMapper.readValue(request.getReader(), clazz);
    }
    
    @Override
    public void write(Object obj, String contentType, HttpServletResponse response) throws IOException {
        response.setContentType(contentType);
        objectMapper.writeValue(response.getWriter(), obj);
    }
} 