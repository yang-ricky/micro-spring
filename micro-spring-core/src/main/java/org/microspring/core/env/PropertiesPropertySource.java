package org.microspring.core.env;

import java.util.Properties;

/**
 * Properties 文件属性源
 */
public class PropertiesPropertySource extends PropertySource<Properties> {

    public PropertiesPropertySource(String name, Properties source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return getSource().getProperty(name);
    }
} 