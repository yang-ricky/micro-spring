package org.microspring.orm.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sort {
    private final List<Order> orders;
    
    public Sort(Order... orders) {
        this.orders = Arrays.asList(orders);
    }
    
    public static Sort by(String... properties) {
        List<Order> orders = new ArrayList<>();
        for (String property : properties) {
            orders.add(Order.asc(property));
        }
        return new Sort(orders.toArray(new Order[0]));
    }
    
    public static Sort by(Order... orders) {
        return new Sort(orders);
    }
    
    public List<Order> getOrders() {
        return orders;
    }
    
    public static class Order {
        private final String property;
        private final Direction direction;
        
        private Order(String property, Direction direction) {
            this.property = property;
            this.direction = direction;
        }
        
        public static Order asc(String property) {
            return new Order(property, Direction.ASC);
        }
        
        public static Order desc(String property) {
            return new Order(property, Direction.DESC);
        }
        
        public String getProperty() { return property; }
        public Direction getDirection() { return direction; }
    }
    
    public enum Direction {
        ASC, DESC
    }
} 