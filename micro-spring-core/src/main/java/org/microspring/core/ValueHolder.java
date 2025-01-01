package org.microspring.core;

public class ValueHolder {
    private final String name;
    private final Object value;
    private final String type;
    
    public ValueHolder(String name, Object value, String type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }
    
    public String getName() { return name; }
    public Object getValue() { return value; }
    public String getType() { return type; }
} 