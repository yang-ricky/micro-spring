package org.microspring.web.context.support;

import org.microspring.context.support.AbstractApplicationContext;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.web.context.WebApplicationContext;

public class AnnotationConfigWebApplicationContext 
        extends AbstractApplicationContext 
        implements WebApplicationContext {
    
    private String[] basePackages;
    
    public AnnotationConfigWebApplicationContext() {
        super(new DefaultBeanFactory());
    }
    
    public AnnotationConfigWebApplicationContext(DefaultBeanFactory beanFactory) {
        super(beanFactory);
    }
    
    public AnnotationConfigWebApplicationContext(String... basePackages) {
        super(new DefaultBeanFactory());
        this.basePackages = basePackages;
        refresh();
    }
    
    @Override
    public String getApplicationName() {
        return "AnnotationConfigWebApplicationContext";
    }
    
    @Override
    public void refresh() {
        if (basePackages != null && basePackages.length > 0) {
            scanPackages(basePackages);
        }
    }
} 