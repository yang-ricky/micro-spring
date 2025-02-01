package org.microspring.core.env;

/**
 * 环境配置接口
 */
public interface Environment {
    
    /**
     * 获取属性值
     */
    String getProperty(String key);

    /**
     * 获取属性值，如果不存在返回默认值
     */
    String getProperty(String key, String defaultValue);

    /**
     * 获取属性值并转换为指定类型
     */
    <T> T getProperty(String key, Class<T> targetType);

    /**
     * 判断是否包含某个属性
     */
    boolean containsProperty(String key);

    /**
     * 获取激活的配置文件
     */
    String[] getActiveProfiles();
} 