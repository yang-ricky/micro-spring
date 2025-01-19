package org.microspring.orm.repository.support;

import java.lang.reflect.Method;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

public class QueryMethodParser {
    private static final Pattern FIND_BY_PATTERN = Pattern.compile("^findBy([A-Z][a-zA-Z0-9]*?)((And|Or)([A-Z][a-zA-Z0-9]*?))*$");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("([A-Z][a-zA-Z0-9]*?)(And|Or)?");
    
    public static <T> QueryMethod parseMethod(Method method, Class<T> entityClass) {
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
        
        // 处理AND和OR条件
        String[] parts = propertyPath.split("(And|Or)");
        System.out.println("Split parts: " + Arrays.toString(parts));
        Pattern operatorPattern = Pattern.compile("And|Or");
        Matcher matcher = operatorPattern.matcher(propertyPath);
        List<String> operators = new ArrayList<>();
        while (matcher.find()) {
            operators.add(matcher.group());
        }
        System.out.println("Operators: " + operators);
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String property = part.substring(0, 1).toLowerCase() + part.substring(1);
            System.out.println("Processing part: [" + part + "] -> property: [" + property + "]");
            
            queryBuilder.append(property).append(" = ?").append(i + 1);
            
            if (i < operators.size()) {
                queryBuilder.append(" ").append(operators.get(i)).append(" ");
            }
            System.out.println("Current query: [" + queryBuilder.toString() + "]");
        }
        
        String query = queryBuilder.toString().trim();
        System.out.println("Final query: [" + query + "]");
        return new QueryMethod(query);
    }
    
    public static boolean isQueryMethod(Method method) {
        return method.getName().startsWith("findBy");
    }
    
    private static class PropertyCondition {
        private final String property;
        private final String connector;
        
        public PropertyCondition(String property, String connector) {
            this.property = property;
            this.connector = connector == null ? "and" : connector.toLowerCase();
        }
        
        public String getProperty() {
            return property;
        }
        
        public String getConnector() {
            return connector;
        }
    }
    
    public static class QueryMethod {
        private final String queryString;
        
        public QueryMethod(String queryString) {
            System.out.println("QueryMethod constructor received: [" + queryString + "]");
            this.queryString = queryString;
        }
        
        public String getQueryString() {
            System.out.println("QueryMethod.getQueryString returning: [" + queryString + "]");
            return queryString;
        }
    }
} 