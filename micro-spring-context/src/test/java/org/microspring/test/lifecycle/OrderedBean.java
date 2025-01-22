package org.microspring.test.lifecycle;

import org.microspring.core.aware.BeanNameAware;
import org.microspring.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderedBean implements BeanNameAware {
    private static List<String> initializationOrder = new ArrayList<>();
    private String beanName;
    
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        initializationOrder.add("setBeanName:" + name);
    }
    
    public void initMe() {
        initializationOrder.add("init:" + beanName);
    }
    
    public void cleanup() {
        initializationOrder.add("cleanup:" + beanName);
    }
    
    public static List<String> getInitializationOrder() {
        return initializationOrder;
    }
    
    public static void clearInitializationOrder() {
        initializationOrder.clear();
    }
} 