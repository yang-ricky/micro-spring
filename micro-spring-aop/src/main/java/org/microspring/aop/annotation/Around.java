package org.microspring.aop.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a method as around advice that can control the target method execution
 * by using ProceedingJoinPoint.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Around {
    /**
     * The pointcut expression where this advice should be applied
     */
    String value();
} 