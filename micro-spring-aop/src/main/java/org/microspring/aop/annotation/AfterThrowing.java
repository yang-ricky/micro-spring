package org.microspring.aop.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a method as after throwing advice that should be executed
 * if the target method throws an exception.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AfterThrowing {
    /**
     * The pointcut expression where this advice should be applied
     */
    String value();
    
    /**
     * The name of the parameter in the advice method that will hold the thrown exception.
     * If specified, the advice method must have a parameter with this name.
     */
    String throwing() default "";
} 