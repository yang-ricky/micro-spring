package org.microspring.core.beans;

public class PropertyValue {
    private final String name;
    private final Object value;
    private final Class<?> type;
    private final boolean isRef;

    public PropertyValue(String name, Object value) {
        this(name, value, null, false);
    }

    public PropertyValue(String name, Object value, Class<?> type) {
        this(name, value, type, false);
    }

    public PropertyValue(String name, Object value, Class<?> type, boolean isRef) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.isRef = isRef;
    }

    public String getName() { return name; }
    public Object getValue() { return value; }
    public Class<?> getType() { return type; }
    public boolean isRef() { return isRef; }
} 