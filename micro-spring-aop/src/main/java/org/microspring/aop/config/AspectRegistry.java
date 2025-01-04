package org.microspring.aop.config;

import org.microspring.aop.Aspect;
import java.util.List;
import java.util.ArrayList;

public class AspectRegistry {
    private final List<Object> aspects = new ArrayList<>();
    
    public void registerAspect(Object aspect) {
        if (aspect.getClass().isAnnotationPresent(Aspect.class)) {
            aspects.add(aspect);
        }
    }
    
    public List<Object> getAspects() {
        return aspects;
    }
} 