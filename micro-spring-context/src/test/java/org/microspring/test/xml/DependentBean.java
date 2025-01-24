package org.microspring.test.xml;

public class DependentBean {
    private TestBean testBean;
    
    public void setTestBean(TestBean testBean) {
        this.testBean = testBean;
    }
    
    public TestBean getTestBean() {
        return testBean;
    }
} 