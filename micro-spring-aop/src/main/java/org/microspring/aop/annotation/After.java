package org.microspring.aop.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a method as after advice that should be executed after the target method,
 * regardless of its outcome (normal or exceptional return).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface After {
    /**
     * The pointcut expression where this advice should be applied
     */
    String value();
} 