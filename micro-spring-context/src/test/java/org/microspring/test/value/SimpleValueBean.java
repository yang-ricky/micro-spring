package org.microspring.test.value;

import org.microspring.beans.factory.annotation.Value;
import org.microspring.stereotype.Component;

@Component
public class SimpleValueBean {
    private final String constructorValue;
    
    @Value("${app.name:defaultName}")
    private String stringWithDefault;
    
    @Value("${app.name}")
    private String stringWithoutDefault;
    
    @Value("${app.port:8080}")
    private int intWithDefault;

    @Value("const")
    private String intWithStringConst;

    @Value("8080")
    private int intWithIntConst;
    
    // 新增: 用于测试setter注入的字段
    private String setterValue;
    private int setterIntValue;
    
    public SimpleValueBean(@Value("constructorValue") String constructorValue) {
        this.constructorValue = constructorValue;
    }
    
    public String getConstructorValue() {
        return constructorValue;
    }
    
    public String getStringWithDefault() { return stringWithDefault; }
    public String getStringWithoutDefault() { return stringWithoutDefault; }
    public int getIntWithDefault() { return intWithDefault; }
    public String getIntWithStringConst() { return intWithStringConst; }
    public int getIntWithIntConst() { return intWithIntConst; }
    
    // 新增: setter方法注入
    @Value("setterInjectedValue")
    public void setSetterValue(String setterValue) {
        this.setterValue = setterValue;
    }
    
    @Value("42")
    public void setSetterIntValue(int setterIntValue) {
        this.setterIntValue = setterIntValue;
    }
    
    public String getSetterValue() {
        return setterValue;
    }
    
    public int getSetterIntValue() {
        return setterIntValue;
    }
} 