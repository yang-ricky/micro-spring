package org.microspring.context.support;

import org.microspring.core.BeanFactory;
import org.microspring.core.spel.SpelExpressionResolver;

public interface ValueResolver {
    Object resolveValue(String expression);
}

class DefaultValueResolver implements ValueResolver {
    private final BeanFactory beanFactory;
    private final SpelExpressionResolver spelResolver;

    public DefaultValueResolver(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.spelResolver = new SpelExpressionResolver();
    }

    @Override
    public Object resolveValue(String expression) {
        if (expression == null) {
            return null;
        }
        
        // 处理 ${property:defaultValue} 格式
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String propertyKey = expression.substring(2, expression.length() - 1);
            String[] parts = propertyKey.split(":", 2);
            String key = parts[0];
            String defaultValue = parts.length > 1 ? parts[1] : null;
            
            String value = System.getProperty(key);
            return value != null ? value : defaultValue;
        }
        
        // 处理 #{expression} 格式 - 使用SpelExpressionResolver
        if (expression.startsWith("#{") && expression.endsWith("}")) {
            return spelResolver.parseExpression(expression, beanFactory);
        }
        
        return expression;
    }
} 