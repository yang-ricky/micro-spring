package org.microspring.core.io;

import org.microspring.core.BeanDefinition;
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
    
    public List<BeanDefinitionHolder> loadBeanDefinitions(String xmlPath) {
        List<BeanDefinitionHolder> holders = new ArrayList<>();
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(xmlPath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);
            
            NodeList beanNodes = doc.getElementsByTagName("bean");
            for (int i = 0; i < beanNodes.getLength(); i++) {
                Element beanElement = (Element) beanNodes.item(i);
                
                String id = beanElement.getAttribute("id");
                String className = beanElement.getAttribute("class");
                String scope = beanElement.getAttribute("scope");
                String initMethod = beanElement.getAttribute("init-method");
                
                Class<?> beanClass = Class.forName(className);
                DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(beanClass);
                
                if (!scope.isEmpty()) {
                    beanDefinition.setScope(scope);
                }
                if (!initMethod.isEmpty()) {
                    beanDefinition.setInitMethodName(initMethod);
                }
                
                // 解析constructor-arg
                NodeList constructorArgs = beanElement.getElementsByTagName("constructor-arg");
                for (int j = 0; j < constructorArgs.getLength(); j++) {
                    Element arg = (Element) constructorArgs.item(j);
                    String ref = arg.getAttribute("ref");
                    String value = arg.getAttribute("value");
                    String type = arg.getAttribute("type");
                    
                    beanDefinition.addConstructorArg(new ConstructorArg(
                        ref,
                        value.isEmpty() ? null : value,
                        Class.forName(type)
                    ));
                }
                
                // 解析property
                NodeList properties = beanElement.getElementsByTagName("property");
                for (int j = 0; j < properties.getLength(); j++) {
                    Element prop = (Element) properties.item(j);
                    handleProperty(prop, beanDefinition);
                }
                
                System.out.println("[XmlBeanDefinitionReader] Loading bean definition: " + id);
                holders.add(new BeanDefinitionHolder(id, beanDefinition));
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error loading XML file: " + xmlPath, e);
        }
        return holders;
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