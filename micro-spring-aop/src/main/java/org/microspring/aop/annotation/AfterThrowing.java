package org.microspring.aop.annotation;

import java.lang.annotation.*;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AfterThrowing {

    String value();
    

    String throwing() default "";
} 