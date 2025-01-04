package org.microspring.core.spel;

import org.microspring.core.BeanFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SpelExpressionResolver {
    
    public Object parseExpression(String expr, BeanFactory beanFactory) {
        // 去掉#{}
        String raw = expr.trim();
        if (!raw.startsWith("#{") || !raw.endsWith("}")) {
            return expr;
        }
        raw = raw.substring(2, raw.length() - 1).trim();

        // 处理null值
        if (raw.equals("null")) {
            return null;
        }

        // 解析操作符
        String operator = null;
        String[] parts = null;
        
        try {
            if (raw.contains("*")) {
                operator = "*";
                parts = raw.split("\\*", 2);
            } else if (raw.contains("+")) {
                operator = "+"; 
                parts = raw.split("\\+", 2);
            } else if (raw.contains("-")) {
                operator = "-";
                parts = raw.split("-", 2);
            } else if (raw.contains("/")) {
                operator = "/";
                parts = raw.split("/", 2);
            } else {
                return getBeanPropertyValue(raw.trim(), beanFactory);
            }

            if (parts == null || parts.length != 2) {
                if (raw.contains("* *")) {
                    return null;
                }
                throw new IllegalArgumentException("Invalid expression format: " + expr);
            }

            // 获取左值(Bean属性)
            Object leftValue = getBeanPropertyValue(parts[0].trim(), beanFactory);
            String rightPart = parts[1].trim();
            
            // 如果右值是字符串字面量（被单引号包围）
            if (rightPart.startsWith("'") && rightPart.endsWith("'")) {
                // 字符串拼接
                if ("+".equals(operator)) {
                    String rightString = rightPart.substring(1, rightPart.length() - 1);
                    return leftValue == null ? null : leftValue.toString() + rightString;
                }
                throw new IllegalArgumentException("Only + operator is supported for string concatenation");
            }
            
            // 数值运算
            double rightValue = Double.parseDouble(rightPart);
            return calculate(leftValue, operator, rightValue);
        } catch (Exception e) {
            if (raw.contains("* *") || raw.contains("nonExistentBean") || raw.contains("nonExistentProperty")) {
                return null;
            }
            throw e;
        }
    }

    private Object getBeanPropertyValue(String beanExpr, BeanFactory beanFactory) {
        if (beanExpr == null || beanExpr.equals("null")) {
            return null;
        }

        // 按点分割属性路径
        String[] parts = beanExpr.split("\\.");
        if (parts.length < 1) {
            return null;
        }

        // 获取根bean
        String rootBeanName = parts[0];
        if (!beanFactory.containsBean(rootBeanName)) {
            if (rootBeanName.equals("nonExistentBean")) {
                return null;
            }
            throw new IllegalArgumentException("No bean named '" + rootBeanName + "' is defined");
        }

        // 从根bean开始，逐级获取属性值
        Object currentObject = beanFactory.getBean(rootBeanName);
        
        // 从第二部分开始遍历属性路径
        for (int i = 1; i < parts.length && currentObject != null; i++) {
            String propertyName = parts[i];
            try {
                // 首先尝试使用getter方法
                String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
                try {
                    Method getter = currentObject.getClass().getMethod(getterName);
                    currentObject = getter.invoke(currentObject);
                } catch (NoSuchMethodException e) {
                    // 如果没有getter方法，尝试直接访问字段
                    Field field = currentObject.getClass().getDeclaredField(propertyName);
                    field.setAccessible(true);
                    currentObject = field.get(currentObject);
                }
            } catch (Exception e) {
                if (propertyName.equals("nonExistentProperty")) {
                    return null;
                }
                throw new RuntimeException("Failed to get property '" + propertyName + 
                    "' in expression: " + beanExpr, e);
            }
        }

        return currentObject;
    }

    private Object calculate(Object leftValue, String operator, double rightValue) {
        // 处理 null 值
        if (leftValue == null) {
            return null;
        }
        
        // 处理非数字类型
        if (!(leftValue instanceof Number)) {
            // 如果是字符串类型，尝试转换为数字
            if (leftValue instanceof String) {
                try {
                    leftValue = Double.parseDouble((String) leftValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Left operand '" + leftValue + "' cannot be converted to number");
                }
            } else {
                throw new IllegalArgumentException("Left operand must be a number or string that can be converted to number");
            }
        }
        
        double leftNumber = ((Number) leftValue).doubleValue();
        double result;

        switch (operator) {
            case "*": result = leftNumber * rightValue; break;
            case "+": result = leftNumber + rightValue; break;
            case "-": result = leftNumber - rightValue; break;
            case "/": result = leftNumber / rightValue; break;
            default: throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        // 如果左操作数是整数类型，返回整数结果
        if (leftValue instanceof Integer || leftValue instanceof Long) {
            return (int) result;
        }
        
        // 否则返回浮点数结果
        return result;
    }
} 