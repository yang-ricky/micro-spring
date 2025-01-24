package org.microspring.test.xml;

public class CircularA {
    private CircularB circularB;
    
    public void setCircularB(CircularB circularB) {
        this.circularB = circularB;
    }
    
    public CircularB getCircularB() {
        return circularB;
    }
} 