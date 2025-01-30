package org.microspring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to bind a method parameter to a request header.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHeader {
    /**
     * The name of the request header to bind to.
     */
    String value() default "";

    /**
     * Whether the header is required.
     * Defaults to true.
     */
    boolean required() default true;

    /**
     * The default value to use if the header is not present.
     */
    String defaultValue() default "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";
} 