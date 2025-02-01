package org.microspring.core.env;

import org.yaml.snakeyaml.Yaml;
import java.util.LinkedHashMap;
import java.util.Map;

public class YamlPropertySource extends PropertySource<Map<String, Object>> {

    public YamlPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        String[] paths = name.split("\\.");
        Map<String, Object> currentMap = getSource();
        
        for (int i = 0; i < paths.length - 1; i++) {
            Object value = currentMap.get(paths[i]);
            if (!(value instanceof Map)) {
                return null;
            }
            currentMap = (Map<String, Object>) value;
        }
        
        return currentMap.get(paths[paths.length - 1]);
    }

    public static YamlPropertySource fromYaml(String name, String yamlContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> properties = yaml.load(yamlContent);
        return new YamlPropertySource(name, properties != null ? properties : new LinkedHashMap<>());
    }

    private Object getFlattenedValue(String path, Object value) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String newPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                getFlattenedValue(newPath, entry.getValue());
            }
            return null;
        }
        return value;
    }
} 