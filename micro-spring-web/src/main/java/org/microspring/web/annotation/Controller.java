package org.microspring.web.annotation;

import java.lang.annotation.*;
import org.microspring.stereotype.Component;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {
    String value() default "";
} 