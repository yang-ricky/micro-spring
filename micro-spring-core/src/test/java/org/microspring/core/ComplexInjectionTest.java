package org.microspring.core;

import org.junit.Test;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class ComplexInjectionTest {
    
    public static class ComplexBean {
        private List<String> stringList;
        private Map<String, Integer> numberMap;
        
        public void setStringList(List<String> list) {
            this.stringList = list;
        }
        
        public void setNumberMap(Map<String, Integer> map) {
            this.numberMap = map;
        }
        
        public List<String> getStringList() { return stringList; }
        public Map<String, Integer> getNumberMap() { return numberMap; }
    }
    
    @Test
    public void testComplexTypeInjection() {
        DefaultBeanFactory factory = new DefaultBeanFactory();
        factory.loadBeanDefinitions("complex-injection.xml");
        
        ComplexBean bean = factory.getBean("complexBean", ComplexBean.class);
        
        assertNotNull(bean);
        assertEquals(3, bean.getStringList().size());
        assertEquals(2, bean.getNumberMap().size());
        assertEquals(Integer.valueOf(42), bean.getNumberMap().get("answer"));
    }
} 