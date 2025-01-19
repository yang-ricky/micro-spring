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
        StringBuilder queryBuilder = new StringBuilder("from ")
            .append(entityClass.getSimpleName())
            .append(" where ");
        
        // 处理AND和OR条件
        String[] parts = propertyPath.split("(And|Or)");
        String[] operators = propertyPath.split("[^AndOr]+");
        operators = operators.length > 0 ? Arrays.copyOfRange(operators, 1, operators.length) : new String[0];
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String property = part.substring(0, 1).toLowerCase() + part.substring(1);
            
            queryBuilder.append(property).append(" = ?").append(i + 1);
            
            if (i < operators.length) {
                queryBuilder.append(" ").append(operators[i]).append(" ");
            }
        }
        
        return new QueryMethod(queryBuilder.toString());
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
            this.queryString = queryString;
        }
        
        public String getQueryString() {
            return queryString;
        }
    }
} 