package org.microspring.core.annotation;

import java.lang.annotation.*;
import org.microspring.core.condition.Condition;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {
    Class<? extends Condition>[] value();
} 