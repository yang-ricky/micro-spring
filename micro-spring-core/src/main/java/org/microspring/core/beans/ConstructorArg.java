package org.microspring.core.beans;

public class ConstructorArg {
    private String ref;
    private Object value;
    private Class<?> type;
    
    public ConstructorArg() {
        // 无参构造函数
    }
    
    public ConstructorArg(String ref, Object value, Class<?> type) {
        this.ref = ref;
        this.value = value;
        this.type = type;
    }
    
    public String getRef() { return ref; }
    public Object getValue() { return value; }
    public Class<?> getType() { return type; }
    
    public boolean isRef() { 
        // 只要有ref就是引用类型，否则就是值类型
        return ref != null && !ref.isEmpty();
    }
} 