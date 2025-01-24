package org.microspring.test.xml;

public class CircularB {
    private CircularA circularA;
    
    public void setCircularA(CircularA circularA) {
        this.circularA = circularA;
    }
    
    public CircularA getCircularA() {
        return circularA;
    }
} 