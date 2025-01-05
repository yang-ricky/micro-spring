package org.microspring.web.http.converter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 消息转换器接口，用于处理 HTTP 请求和响应的转换
 */
public interface HttpMessageConverter<T> {
    
    /**
     * 判断是否可以读取指定的类型
     */
    boolean canRead(Class<?> clazz, String mediaType);
    
    /**
     * 判断是否可以写入指定的类型
     */
    boolean canWrite(Class<?> clazz, String mediaType);
    
    /**
     * 从请求中读取数据并转换为对象
     */
    T read(Class<? extends T> clazz, HttpServletRequest request) throws IOException;
    
    /**
     * 将对象写入响应
     */
    void write(T t, String contentType, HttpServletResponse response) throws IOException;
} 