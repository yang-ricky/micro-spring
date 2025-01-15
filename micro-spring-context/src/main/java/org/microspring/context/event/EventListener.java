package org.microspring.context.event;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {
    Class<?>[] value() default {};
} 