package org.microspring.core.env;

/**
 * 属性源的抽象基类
 */
public abstract class PropertySource<T> {
    private final String name;
    private final T source;

    public PropertySource(String name, T source) {
        this.name = name;
        this.source = source;
    }

    public String getName() {
        return this.name;
    }

    protected T getSource() {
        return this.source;
    }

    public abstract Object getProperty(String name);

    public boolean containsProperty(String name) {
        return getProperty(name) != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {name='" + this.name + "'}";
    }
} 