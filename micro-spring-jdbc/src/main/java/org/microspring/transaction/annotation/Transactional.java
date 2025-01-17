package org.microspring.transaction.annotation;

import org.microspring.transaction.TransactionDefinition;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
    int propagation() default TransactionDefinition.PROPAGATION_REQUIRED;
    int isolation() default TransactionDefinition.ISOLATION_DEFAULT;
    boolean readOnly() default false;
    Class<? extends Throwable>[] rollbackFor() default {};
} 