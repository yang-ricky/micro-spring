package org.microspring.core.io;

import org.microspring.core.BeanDefinition;
import org.microspring.core.BeanFactory;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.beans.RuntimeBeanReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlBeanDefinitionReader {
    private final DefaultBeanFactory beanFactory;
    
    public XmlBeanDefinitionReader(DefaultBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    
    public void loadBeanDefinitions(String location) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(location);
        if (inputStream == null) {
            throw new RuntimeException("Resource not found: " + location);
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(inputStream);
            
            Element root = doc.getDocumentElement();
            NodeList nl = root.getElementsByTagName("bean");
            
            for (int i = 0; i < nl.getLength(); i++) {
                Element ele = (Element) nl.item(i);
                String id = ele.getAttribute("id");
                String className = ele.getAttribute("class");
                
                Class<?> clz = Class.forName(className);
                DefaultBeanDefinition bd = new DefaultBeanDefinition(clz);
                
                // 解析scope属性
                String scope = ele.getAttribute("scope");
                if (scope != null && !scope.isEmpty()) {
                    bd.setScope(scope);
                }
                
                String lazyInit = ele.getAttribute("lazy-init");
                if ("true".equals(lazyInit)) {
                    bd.setLazyInit(true);
                }
                
                // 解析生命周期方法
                String initMethod = ele.getAttribute("init-method");
                if (initMethod != null && !initMethod.isEmpty()) {
                    bd.setInitMethodName(initMethod);
                }
                
                String destroyMethod = ele.getAttribute("destroy-method");
                if (destroyMethod != null && !destroyMethod.isEmpty()) {
                    bd.setDestroyMethodName(destroyMethod);
                }
                
                // 处理构造器参数
                NodeList constructorNodes = ele.getElementsByTagName("constructor-arg");
                
                for (int j = 0; j < constructorNodes.getLength(); j++) {
                    Element constructorEle = (Element) constructorNodes.item(j);
                    String ref = constructorEle.getAttribute("ref");
                    String value = constructorEle.getAttribute("value");
                    
                    if (ref != null && !ref.isEmpty()) {
                        ConstructorArg arg = new ConstructorArg(ref, null, Object.class);
                        bd.addConstructorArg(arg);
                    } else if (value != null && !value.isEmpty()) {
                        ConstructorArg arg = new ConstructorArg(null, value, String.class);
                        bd.addConstructorArg(arg);
                    }
                }
                
                // 处理属性注入
                NodeList propertyNodes = ele.getElementsByTagName("property");
                
                for (int j = 0; j < propertyNodes.getLength(); j++) {
                    Element propEle = (Element) propertyNodes.item(j);
                    handleProperty(propEle, bd);
                }
                
                beanFactory.registerBeanDefinition(id, bd);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading XML file: " + location, e);
        }
    }
    
    private void handleProperty(Element propElement, BeanDefinition bd) {
        String name = propElement.getAttribute("name");
        String value = propElement.getAttribute("value");
        String ref = propElement.getAttribute("ref");
        
        // 处理List类型的属性
        NodeList listNodes = propElement.getElementsByTagName("list");
        if (listNodes.getLength() > 0) {
            Element listElement = (Element) listNodes.item(0);
            List<Object> list = parseListElement(listElement);
            // 如果列表中包含引用，将整个属性标记为引用类型
            boolean containsRefs = listElement.getElementsByTagName("ref").getLength() > 0;
            bd.addPropertyValue(new PropertyValue(name, list, List.class, containsRefs));
            return;
        }
        
        // 处理Map类型的属性
        NodeList mapNodes = propElement.getElementsByTagName("map");
        if (mapNodes.getLength() > 0) {
            Element mapElement = (Element) mapNodes.item(0);
            Map<String, Object> map = parseMapElement(mapElement);
            // 如果Map中包含引用，将整个属性标记为引用类型
            boolean containsRefs = mapElement.getElementsByTagName("ref").getLength() > 0 || 
                                 hasAttributeWithValue(mapElement, "value-ref");
            bd.addPropertyValue(new PropertyValue(name, map, Map.class, containsRefs));
            return;
        }
        
        // 处理引用类型的属性
        if (ref != null && !ref.isEmpty()) {
            bd.addPropertyValue(new PropertyValue(name, ref, null, true));
            return;
        }
        
        // 处理普通值类型的属性
        if (value != null && !value.isEmpty()) {
            bd.addPropertyValue(new PropertyValue(name, value, String.class));
        }
    }
    
    private boolean hasAttributeWithValue(Element element, String attributeName) {
        NodeList entries = element.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            String value = entry.getAttribute(attributeName);
            if (value != null && !value.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    private List<Object> parseListElement(Element listElement) {
        List<Object> list = new ArrayList<>();
        
        // 处理值类型
        NodeList values = listElement.getElementsByTagName("value");
        for (int i = 0; i < values.getLength(); i++) {
            list.add(values.item(i).getTextContent());
        }
        
        // 处理引用类型
        NodeList refs = listElement.getElementsByTagName("ref");
        for (int i = 0; i < refs.getLength(); i++) {
            Element refElement = (Element) refs.item(i);
            String beanRef = refElement.getAttribute("bean");
            list.add(beanRef);  // 直接添加引用名称，不使用 RuntimeBeanReference
        }
        
        return list;
    }
    
    private Map<String, Object> parseMapElement(Element mapElement) {
        Map<String, Object> map = new HashMap<>();
        NodeList entries = mapElement.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            String key = entry.getAttribute("key");
            
            // 1. 检查是否有 value-ref 属性
            String valueRef = entry.getAttribute("value-ref");
            if (valueRef != null && !valueRef.isEmpty()) {
                map.put(key, valueRef);  // 直接添加引用名称
                continue;
            }
            
            // 2. 检查是否有 value 属性
            String value = entry.getAttribute("value");
            if (value != null && !value.isEmpty()) {
                try {
                    map.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    map.put(key, value);
                }
                continue;
            }
            
            // 3. 检查是否有 ref 子标签
            NodeList refNodes = entry.getElementsByTagName("ref");
            if (refNodes.getLength() > 0) {
                Element refElement = (Element) refNodes.item(0);
                String beanRef = refElement.getAttribute("bean");
                map.put(key, beanRef);  // 直接添加引用名称
                continue;
            }
            
            // 4. 检查是否有 value 子标签
            NodeList valueNodes = entry.getElementsByTagName("value");
            if (valueNodes.getLength() > 0) {
                String textContent = valueNodes.item(0).getTextContent();
                try {
                    map.put(key, Integer.parseInt(textContent));
                } catch (NumberFormatException e) {
                    map.put(key, textContent);
                }
            }
        }
        return map;
    }
} 