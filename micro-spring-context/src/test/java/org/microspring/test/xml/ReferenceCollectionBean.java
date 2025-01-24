package org.microspring.test.xml;

import java.util.List;
import java.util.Map;

public class ReferenceCollectionBean {
    private List<TestBean> testBeans;
    private Map<String, TestBean> testBeanMap;
    
    public void setTestBeans(List<TestBean> testBeans) {
        this.testBeans = testBeans;
    }
    
    public List<TestBean> getTestBeans() {
        return testBeans;
    }
    
    public void setTestBeanMap(Map<String, TestBean> testBeanMap) {
        this.testBeanMap = testBeanMap;
    }
    
    public Map<String, TestBean> getTestBeanMap() {
        return testBeanMap;
    }
} 