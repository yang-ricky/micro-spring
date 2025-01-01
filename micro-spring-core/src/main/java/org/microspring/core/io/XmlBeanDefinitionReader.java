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
                    parsePropertyElement(prop, beanDefinition);
                }
                
                System.out.println("[XmlBeanDefinitionReader] Loading bean definition: " + id);
                holders.add(new BeanDefinitionHolder(id, beanDefinition));
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error loading XML file: " + xmlPath, e);
        }
        return holders;
    }
    
    private void parsePropertyElement(Element property, BeanDefinition bd) {
        String name = property.getAttribute("name");
        String value = property.getAttribute("value");
        String ref = property.getAttribute("ref");
        
        // 处理复杂类型
        NodeList listNodes = property.getElementsByTagName("list");
        NodeList mapNodes = property.getElementsByTagName("map");
        
        if (listNodes.getLength() > 0) {
            Element listElement = (Element) listNodes.item(0);
            List<String> list = parseListElement(listElement);
            bd.addPropertyValue(new PropertyValue(name, list));
        } else if (mapNodes.getLength() > 0) {
            Element mapElement = (Element) mapNodes.item(0);
            Map<String, String> map = new HashMap<>();
            NodeList entries = mapElement.getElementsByTagName("entry");
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                String key = entry.getAttribute("key");
                String mapValue = entry.getAttribute("value");
                map.put(key, mapValue);
            }
            bd.addPropertyValue(new PropertyValue(name, map));
        } else if (ref != null && !ref.isEmpty()) {
            bd.addPropertyValue(new PropertyValue(name, ref, null));
        } else {
            bd.addPropertyValue(new PropertyValue(name, value));
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
} 