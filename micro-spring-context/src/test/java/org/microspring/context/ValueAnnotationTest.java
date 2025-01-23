package org.microspring.context;

import org.junit.Test;
import org.microspring.beans.factory.annotation.Value;
import org.microspring.stereotype.Component;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.test.value.SimpleValueBean;
import static org.junit.Assert.*;

public class ValueAnnotationTest {
    
    @Test
    public void testPropertyResolution() {
        // 设置系统属性
        System.setProperty("app.name", "TestApp");
        System.setProperty("app.port", "9090");
        
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.value");
        
        SimpleValueBean bean = context.getBean(SimpleValueBean.class);
        assertNotNull("Bean should not be null", bean);
        
        // 测试构造器值
        assertEquals("Constructor value should be resolved", 
            "constructorValue", bean.getConstructorValue());
            
        // 测试属性解析（有默认值）
        assertEquals("Property with value should override default", 
            "TestApp", bean.getStringWithDefault());
        
        // 测试属性解析（无默认值）
        assertEquals("Property without default should be resolved", 
            "TestApp", bean.getStringWithoutDefault());
        
        // 测试数字类型转换
        assertEquals("Integer property should be converted", 
            9090, bean.getIntWithDefault());

        assertEquals("String constant should be set", 
            "const", bean.getIntWithStringConst());

        assertEquals("Integer constant should be converted", 
            8080, bean.getIntWithIntConst());
            
        // 测试setter方法注入
        assertEquals("Setter string value should be injected", 
            "setterInjectedValue", bean.getSetterValue());
            
        assertEquals("Setter int value should be injected and converted", 
            42, bean.getSetterIntValue());
    }
    
    @Test
    public void testDefaultValues() {
        // 清除系统属性
        System.clearProperty("app.name");
        System.clearProperty("app.port");
        
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.value");
        
        SimpleValueBean bean = context.getBean(SimpleValueBean.class);
        assertNotNull("Bean should not be null", bean);
        
        // 测试构造器值
        assertEquals("Constructor value should be resolved", 
            "constructorValue", bean.getConstructorValue());
            
        // 测试默认值
        assertEquals("Default value should be used when property is missing", 
            "defaultName", bean.getStringWithDefault());
        assertNull("Null should be returned when no default and property missing", 
            bean.getStringWithoutDefault());
        assertEquals("Default port should be used", 
            8080, bean.getIntWithDefault());
            
        // 测试setter方法注入
        assertEquals("Setter string value should be injected", 
            "setterInjectedValue", bean.getSetterValue());
            
        assertEquals("Setter int value should be injected and converted", 
            42, bean.getSetterIntValue());
    }
} 