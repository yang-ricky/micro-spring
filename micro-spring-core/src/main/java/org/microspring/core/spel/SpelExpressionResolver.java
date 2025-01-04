package org.microspring.core.spel;

import org.microspring.core.BeanFactory;

import java.lang.reflect.Field;

public class SpelExpressionResolver {
    
    public Object parseExpression(String expr, BeanFactory beanFactory) {
        // 去掉#{}
        String raw = expr.trim();
        if (!raw.startsWith("#{") || !raw.endsWith("}")) {
            return expr;
        }
        raw = raw.substring(2, raw.length() - 1).trim();

        // 解析操作符
        String operator = null;
        String[] parts;
        if (raw.contains("*")) {
            operator = "*";
            parts = raw.split("\\*");
        } else if (raw.contains("+")) {
            operator = "+"; 
            parts = raw.split("\\+");
        } else if (raw.contains("-")) {
            operator = "-";
            parts = raw.split("-");
        } else if (raw.contains("/")) {
            operator = "/";
            parts = raw.split("/");
        } else {
            // 没有运算符,直接获取bean属性
            return getBeanPropertyValue(raw.trim(), beanFactory);
        }

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid expression format: " + expr);
        }

        // 获取左值(Bean属性)
        Object leftValue = getBeanPropertyValue(parts[0].trim(), beanFactory);
        
        // 获取右值(数字)
        double rightValue = Double.parseDouble(parts[1].trim());

        // 计算结果
        return calculate(leftValue, operator, rightValue);
    }

    private Object getBeanPropertyValue(String beanExpr, BeanFactory beanFactory) {
        String[] beanParts = beanExpr.split("\\.");
        if (beanParts.length != 2) {
            throw new IllegalArgumentException("Invalid bean property expression: " + beanExpr);
        }

        String beanName = beanParts[0];
        String fieldName = beanParts[1];

        Object bean = beanFactory.getBean(beanName);
        try {
            Field field = bean.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(bean);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get property value: " + beanExpr, e);
        }
    }

    private Object calculate(Object leftValue, String operator, double rightValue) {
        if (!(leftValue instanceof Number)) {
            throw new IllegalArgumentException("Left operand must be a number");
        }
        double leftNumber = ((Number) leftValue).doubleValue();

        switch (operator) {
            case "*": return leftNumber * rightValue;
            case "+": return leftNumber + rightValue;
            case "-": return leftNumber - rightValue;
            case "/": return leftNumber / rightValue;
            default: throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }
} 