package org.microspring.core.condition;

public interface Condition {
    boolean matches(ConditionContext context);
} 