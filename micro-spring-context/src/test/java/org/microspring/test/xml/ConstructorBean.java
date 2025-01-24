package org.microspring.test.xml;

public class ConstructorBean {
    private final TestBean testBean;
    
    public ConstructorBean(TestBean testBean) {
        this.testBean = testBean;
    }
    
    public String getName() {
        return testBean.getName();
    }
} 