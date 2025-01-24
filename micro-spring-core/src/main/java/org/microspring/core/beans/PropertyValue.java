package org.microspring.core.beans;

public class PropertyValue {
    private final String name;
    private final Object value;
    private final Class<?> type;
    private final boolean isRef;

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
    
    public Object getRef() {
        if (!isRef) {
            return null;
        }
        // 如果是普通引用，返回字符串
        if (value instanceof String) {
            return value;
        }
        // 如果是集合引用，直接返回集合
        return value;
    }
} 