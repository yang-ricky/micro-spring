package org.microspring.core.condition;

public class WindowsCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context) {
        String osName = context.getEnvironment("os.name");
        return osName != null && osName.toLowerCase().contains("windows");
    }
} 