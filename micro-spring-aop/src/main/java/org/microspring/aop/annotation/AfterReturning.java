package org.microspring.aop.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a method as after returning advice that should be executed
 * after the target method successfully returns.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AfterReturning {
    /**
     * The pointcut expression where this advice should be applied
     */
    String value();
    
    /**
     * The name of the parameter in the advice method that will hold the return value.
     * If specified, the advice method must have a parameter with this name.
     */
    String returning() default "";
} 