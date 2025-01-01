package org.microspring.core.beans;

public class ConstructorArg {
    private final String ref;
    private final Object value;
    private final Class<?> type;
    
    public ConstructorArg(String ref, Object value, Class<?> type) {
        this.ref = ref;
        this.value = value;
        this.type = type;
    }
    
    public String getRef() { return ref; }
    public Object getValue() { return value; }
    public Class<?> getType() { return type; }
    public boolean isRef() { return ref != null && !ref.isEmpty(); }
} 