package org.microspring.context.support;

public class ClassPathXmlApplicationContext extends AbstractApplicationContext {
    private final String configLocation;

    public ClassPathXmlApplicationContext(String configLocation) {
        this.configLocation = configLocation;
        refresh();
    }

    @Override
    public void refresh() {
        beanFactory.loadBeanDefinitions(configLocation);
    }

    @Override
    public String getApplicationName() {
        return "ClassPathXmlApplicationContext";
    }
} 