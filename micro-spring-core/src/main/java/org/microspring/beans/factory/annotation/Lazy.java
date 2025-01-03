package org.microspring.beans.factory.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lazy {
    boolean value() default true;
} 