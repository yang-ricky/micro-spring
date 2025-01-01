package org.microspring.context.support;

public interface ValueResolver {
    Object resolveValue(String expression);
}

class DefaultValueResolver implements ValueResolver {
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
        
        // 处理 #{expression} 格式
        if (expression.startsWith("#{") && expression.endsWith("}")) {
            String math = expression.substring(2, expression.length() - 1).trim();
            return evaluateExpression(math);
        }
        
        return expression;
    }
    
    private Object evaluateExpression(String expression) {
        // 简单的数学表达式计算
        String[] parts = expression.split("\\+");
        if (parts.length == 2) {
            return Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
        }
        return expression;
    }
} 