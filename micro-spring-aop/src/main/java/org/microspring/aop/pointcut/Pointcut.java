package org.microspring.aop.pointcut;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Pointcut {
    String value(); // 切点表达式，如"execution(* com.example.service.*.*(..))"
} 