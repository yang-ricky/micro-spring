package org.microspring.core.env;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 标准环境实现
 */
public class StandardEnvironment implements Environment {
    
    private final List<PropertySource<?>> propertySources = new ArrayList<>();
    private final String[] activeProfiles;

    public StandardEnvironment() {
        this(new String[]{"default"});
    }

    public StandardEnvironment(String[] activeProfiles) {
        this.activeProfiles = activeProfiles;
        loadDefaultProperties();
    }

    private void loadDefaultProperties() {
        // 1. 先加载默认的 YAML 配置（优先级最低）
        loadYamlConfig("application.yaml");
        loadYamlConfig("application.yml");
        
        // 2. 加载默认的 properties 配置（优先级高于默认YAML）
        loadPropertiesConfig("application.properties");

        // 3. 加载 profile 相关的配置（优先级最高）
        for (String profile : activeProfiles) {
            // 先加载 profile 的 YAML
            loadYamlConfig("application-" + profile + ".yaml");
            loadYamlConfig("application-" + profile + ".yml");
            // 后加载 profile 的 properties（优先级高于YAML）
            loadPropertiesConfig("application-" + profile + ".properties");
        }
    }

    private void loadYamlConfig(String resourcePath) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input != null) {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                YamlPropertySource yamlSource = YamlPropertySource.fromYaml(resourcePath, content.toString());
                // YAML配置添加到列表末尾，优先级较低
                propertySources.add(yamlSource);
            }
        } catch (IOException e) {
            // 如果文件不存在或无法读取，忽略异常
        }
    }

    private void loadPropertiesConfig(String resourcePath) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input != null) {
                Properties properties = new Properties();
                properties.load(input);
                // Properties配置添加到列表开头，优先级较高
                propertySources.add(0, new PropertiesPropertySource(resourcePath, properties));
            }
        } catch (IOException e) {
            // 如果文件不存在或无法读取，忽略异常
        }
    }

    @Override
    public String getProperty(String key) {
        for (PropertySource<?> propertySource : propertySources) {
            Object value = propertySource.getProperty(key);
            if (value != null) {
                return value.toString().trim();
            }
        }
        return null;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }

        if (targetType == String.class) {
            return (T) value;
        } else if (targetType == Integer.class) {
            return (T) Integer.valueOf(value);
        } else if (targetType == Long.class) {
            return (T) Long.valueOf(value);
        } else if (targetType == Boolean.class) {
            return (T) Boolean.valueOf(value);
        }
        
        throw new IllegalArgumentException("Unsupported target type: " + targetType);
    }

    @Override
    public boolean containsProperty(String key) {
        return getProperty(key) != null;
    }

    @Override
    public String[] getActiveProfiles() {
        return this.activeProfiles;
    }

    public void addPropertySource(PropertySource<?> propertySource) {
        propertySources.add(0, propertySource);
    }
} 