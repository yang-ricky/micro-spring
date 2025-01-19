package org.microspring.core.io;

import org.microspring.core.BeanDefinition;
import org.microspring.core.BeanFactory;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
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
            List<String> list = parseListElement(listElement);
            bd.addPropertyValue(new PropertyValue(name, list, List.class));
            return;
        }
        
        // 处理Map类型的属性
        NodeList mapNodes = propElement.getElementsByTagName("map");
        if (mapNodes.getLength() > 0) {
            Element mapElement = (Element) mapNodes.item(0);
            Map<String, Object> map = parseMapElement(mapElement);
            bd.addPropertyValue(new PropertyValue(name, map, Map.class));
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
    
    private List<String> parseListElement(Element listElement) {
        List<String> list = new ArrayList<>();
        NodeList values = listElement.getElementsByTagName("value");
        for (int i = 0; i < values.getLength(); i++) {
            list.add(values.item(i).getTextContent());
        }
        return list;
    }
    
    private Map<String, Object> parseMapElement(Element mapElement) {
        Map<String, Object> map = new HashMap<>();
        NodeList entries = mapElement.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            String key = entry.getAttribute("key");
            String strValue = entry.getAttribute("value");
            
            // 尝试将值转换为整数
            try {
                map.put(key, Integer.parseInt(strValue));
            } catch (NumberFormatException e) {
                // 如果转换失败，保持字符串类型
                map.put(key, strValue);
            }
        }
        return map;
    }
} 