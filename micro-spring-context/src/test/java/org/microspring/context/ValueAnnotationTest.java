package org.microspring.context;

import org.junit.Test;
import org.microspring.beans.factory.annotation.Value;
import org.microspring.stereotype.Component;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import static org.junit.Assert.*;

public class ValueAnnotationTest {
    
    @Component
    public static class SimpleValueBean {
        // private String constructorValue;

        // public SimpleValueBean(@Value("constructorValue") String constructorValue) {
        //     this.constructorValue = constructorValue;
        // }

        // public String getConstructorValue() {
        //     return constructorValue;
        // }

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
        
        public String getStringWithDefault() { return stringWithDefault; }
        public String getStringWithoutDefault() { return stringWithoutDefault; }
        public int getIntWithDefault() { return intWithDefault; }
        public String getIntWithStringConst() { return intWithStringConst; }
        public int getIntWithIntConst() { return intWithIntConst; }
    }
    
    @Test
    public void testPropertyResolution() {
        // 设置系统属性
        System.setProperty("app.name", "TestApp");
        System.setProperty("app.port", "9090");
        
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        SimpleValueBean bean = context.getBean(SimpleValueBean.class);
        assertNotNull("Bean should not be null", bean);
        
        // 测试属性解析（有默认值）
        assertEquals("Property with value should override default", 
            "TestApp", bean.getStringWithDefault());
        
        // 测试属性解析（无默认值）
        assertEquals("Property without default should be resolved", 
            "TestApp", bean.getStringWithoutDefault());
        
        // 测试数字类型转换
        assertEquals("Integer property should be converted", 
            9090, bean.getIntWithDefault());

        assertEquals("Integer property should be converted", 
            "const", bean.getIntWithStringConst());

        assertEquals("Integer property should be converted", 
            8080, bean.getIntWithIntConst());

        // assertEquals("Constructor value should be resolved", 
        //     "constructorValue", bean.getConstructorValue());
    }
    
    @Test
    public void testDefaultValues() {
        // 清除系统属性
        System.clearProperty("app.name");
        System.clearProperty("app.port");
        
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        SimpleValueBean bean = context.getBean(SimpleValueBean.class);
        
        // 测试默认值
        assertEquals("Default value should be used when property is missing", 
            "defaultName", bean.getStringWithDefault());
        assertNull("Null should be returned when no default and property missing", 
            bean.getStringWithoutDefault());
        assertEquals("Default port should be used", 
            8080, bean.getIntWithDefault());
    }
} 