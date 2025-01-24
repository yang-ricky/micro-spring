package org.microspring.test.xml;

public class OuterBean {
    private TestBean innerBean;
    
    public void setInnerBean(TestBean innerBean) {
        this.innerBean = innerBean;
    }
    
    public TestBean getInnerBean() {
        return innerBean;
    }
} 