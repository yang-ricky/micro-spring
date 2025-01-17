package org.microspring.context.support;

import org.microspring.core.io.XmlBeanDefinitionReader;

public class ClassPathXmlApplicationContext extends AbstractApplicationContext {
    private final String configLocation;

    public ClassPathXmlApplicationContext(String configLocation) {
        super();
        this.configLocation = configLocation;
        refresh();
    }

    @Override
    public void refresh() {
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(getBeanFactory());
        reader.loadBeanDefinitions(configLocation);
        
        super.refresh();
    }

    @Override
    public String getApplicationName() {
        return "ClassPathXmlApplicationContext";
    }
} 