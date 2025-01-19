package org.microspring.orm.repository.support;

import java.lang.reflect.Method;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import org.microspring.orm.repository.Pageable;
import org.microspring.orm.repository.Query;

public class QueryMethodParser {
    // 支持的操作符
    private static final String[] OPERATORS = {
        "Equals", "",  // 空字符串表示默认的Equals
        "Like",
        "NotLike",
        "LessThan",
        "GreaterThan",
        "LessThanEqual",
        "GreaterThanEqual",
        "Between",
        "In",
        "NotIn",
        "IsNull",
        "IsNotNull"
    };
    
    private static final Pattern FIND_BY_PATTERN = Pattern.compile("^findBy([A-Z][a-zA-Z0-9]*?)((And|Or)([A-Z][a-zA-Z0-9]*?))*$");
    
    public static QueryMethod parseMethod(Method method, Class<?> entityClass) {
        // 首先检查是否有@Query注解
        Query queryAnnotation = method.getAnnotation(Query.class);
        if (queryAnnotation != null) {
            // 如果有@Query注解，直接使用注解中的查询语句
            return new QueryMethod(queryAnnotation.value(), hasPageableParameter(method));
        }
        
        // 没有@Query注解，继续使用方法名解析
        String methodName = method.getName();
        if (!methodName.startsWith("findBy")) {
            throw new IllegalArgumentException("Not a valid query method: " + methodName);
        }
        
        String propertyPath = methodName.substring("findBy".length());
        System.out.println("Method name: [" + methodName + "]");
        System.out.println("Property path: [" + propertyPath + "]");
        
        StringBuilder queryBuilder = new StringBuilder("from ")
            .append(entityClass.getSimpleName())
            .append(" where ");
            
        // 解析属性和操作符
        List<PropertyOperation> operations = parsePropertyOperations(propertyPath);
        List<String> logicalOperators = new ArrayList<>();
        
        // 提取逻辑操作符(And/Or)
        Pattern logicPattern = Pattern.compile("And|Or");
        Matcher logicMatcher = logicPattern.matcher(propertyPath);
        while (logicMatcher.find()) {
            logicalOperators.add(logicMatcher.group().toLowerCase());
        }
        
        // 处理第一个条件
        appendOperation(queryBuilder, operations.get(0), 1);
        
        // 处理后续条件
        for (int i = 1; i < operations.size(); i++) {
            queryBuilder.append(" ").append(logicalOperators.get(i-1)).append(" ");
            appendOperation(queryBuilder, operations.get(i), i + 1);
        }
        
        // 检查是否有分页参数
        boolean hasPageable = false;
        if (method.getParameters().length > 0) {
            Class<?> lastParam = method.getParameters()[method.getParameters().length - 1].getType();
            hasPageable = Pageable.class.isAssignableFrom(lastParam);
        }
        
        String query = queryBuilder.toString().trim();
        
        // 如果有Sort参数，添加ORDER BY子句的占位符
        if (hasPageable) {
            query += " #{orderBy}";
        }
        
        System.out.println("Final query: [" + query + "]");
        return new QueryMethod(query, hasPageable);
    }
    
    private static List<PropertyOperation> parsePropertyOperations(String propertyPath) {
        List<PropertyOperation> operations = new ArrayList<>();
        String[] parts = propertyPath.split("(And|Or)");
        
        for (String part : parts) {
            String property = null;
            String operator = "Equals"; // 默认操作符
            
            // 检查是否包含操作符
            for (String op : OPERATORS) {
                if (!op.isEmpty() && part.endsWith(op)) {
                    property = part.substring(0, part.length() - op.length());
                    operator = op;
                    break;
                }
            }
            
            // 如果没找到操作符，整个部分就是属性名
            if (property == null) {
                property = part;
            }
            
            // 转换属性名首字母为小写
            property = property.substring(0, 1).toLowerCase() + property.substring(1);
            operations.add(new PropertyOperation(property, operator));
        }
        
        return operations;
    }
    
    private static void appendOperation(StringBuilder queryBuilder, PropertyOperation operation, int paramIndex) {
        String property = operation.getProperty();
        String operator = operation.getOperator();
        
        switch (operator) {
            case "Like":
                queryBuilder.append(property).append(" like ?").append(paramIndex);
                break;
            case "NotLike":
                queryBuilder.append(property).append(" not like ?").append(paramIndex);
                break;
            case "LessThan":
                queryBuilder.append(property).append(" < ?").append(paramIndex);
                break;
            case "GreaterThan":
                queryBuilder.append(property).append(" > ?").append(paramIndex);
                break;
            case "Between":
                queryBuilder.append(property).append(" between ?")
                    .append(paramIndex).append(" and ?").append(paramIndex + 1);
                break;
            case "IsNull":
                queryBuilder.append(property).append(" is null");
                break;
            case "IsNotNull":
                queryBuilder.append(property).append(" is not null");
                break;
            default: // Equals
                queryBuilder.append(property).append(" = ?").append(paramIndex);
                break;
        }
    }
    
    private static class PropertyOperation {
        private final String property;
        private final String operator;
        
        public PropertyOperation(String property, String operator) {
            this.property = property;
            this.operator = operator;
        }
        
        public String getProperty() { return property; }
        public String getOperator() { return operator; }
    }
    
    public static boolean isQueryMethod(Method method) {
        return method.getName().startsWith("findBy") || method.isAnnotationPresent(Query.class);
    }
    
    public static class QueryMethod {
        private final String queryString;
        private final boolean pageable;
        
        public QueryMethod(String queryString, boolean pageable) {
            System.out.println("QueryMethod constructor received: [" + queryString + "]");
            this.queryString = queryString;
            this.pageable = pageable;
        }
        
        public String getQueryString() {
            System.out.println("QueryMethod.getQueryString returning: [" + queryString + "]");
            return queryString;
        }
        
        public boolean isPageable() {
            return pageable;
        }
    }
    
    private static boolean hasPageableParameter(Method method) {
        if (method.getParameters().length > 0) {
            Class<?> lastParam = method.getParameters()[method.getParameters().length - 1].getType();
            return Pageable.class.isAssignableFrom(lastParam);
        }
        return false;
    }
} 