package org.microspring.beans.factory.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {
    String value() default "singleton";
    
    // 添加作用域常量
    public static final String SINGLETON = "singleton";
    public static final String PROTOTYPE = "prototype";
    public static final String REQUEST = "request";
    public static final String SESSION = "session";
} 