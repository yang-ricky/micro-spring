package org.microspring.core.io;

import org.microspring.core.BeanDefinition;
import org.microspring.core.DefaultBeanDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XmlBeanDefinitionReader {
    
    public List<BeanDefinitionHolder> loadBeanDefinitions(String xmlPath) {
        List<BeanDefinitionHolder> holders = new ArrayList<>();
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(xmlPath);
            if (is == null) {
                throw new RuntimeException("Cannot find xml file: " + xmlPath);
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            
            NodeList beanNodes = doc.getElementsByTagName("bean");
            for (int i = 0; i < beanNodes.getLength(); i++) {
                Element beanElement = (Element) beanNodes.item(i);
                
                String id = beanElement.getAttribute("id");
                String className = beanElement.getAttribute("class");
                String scope = beanElement.getAttribute("scope");
                String initMethod = beanElement.getAttribute("init-method");
                
                // 创建BeanDefinition
                Class<?> beanClass = Class.forName(className);
                DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(beanClass);
                
                if (!scope.isEmpty()) {
                    beanDefinition.setScope(scope);
                }
                if (!initMethod.isEmpty()) {
                    beanDefinition.setInitMethodName(initMethod);
                }
                
                System.out.println("[XmlBeanDefinitionReader] Loading bean definition: " + id);
                holders.add(new BeanDefinitionHolder(id, beanDefinition));
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error loading XML file: " + xmlPath, e);
        }
        return holders;
    }
} 