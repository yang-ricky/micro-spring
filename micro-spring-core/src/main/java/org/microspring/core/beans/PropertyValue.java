package org.microspring.core.beans;

public class PropertyValue {
    private final String name;
    private final String ref;
    private final Object value;
    
    public PropertyValue(String name, String ref, Object value) {
        this.name = name;
        this.ref = ref;
        this.value = value;
    }
    
    public String getName() { return name; }
    public String getRef() { return ref; }
    public Object getValue() { return value; }
    public boolean isRef() { return ref != null && !ref.isEmpty(); }
} 