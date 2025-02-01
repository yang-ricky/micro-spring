package org.microspring.aop.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a method as before advice that should be executed before the target method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Before {
    /**
     * The pointcut expression where this advice should be applied
     */
    String value();
} 