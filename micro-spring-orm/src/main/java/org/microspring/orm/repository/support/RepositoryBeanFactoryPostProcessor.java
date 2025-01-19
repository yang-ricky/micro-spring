package org.microspring.orm.repository.support;

import org.microspring.orm.OrmTemplate;
import org.microspring.orm.repository.OrmRepository;
import org.microspring.orm.repository.Repository;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RepositoryBeanFactoryPostProcessor {
    
    private final String basePackage;
    private final OrmTemplate ormTemplate;
    private final List<Object> repositories = new ArrayList<>();
    
    public RepositoryBeanFactoryPostProcessor(String basePackage, OrmTemplate ormTemplate) {
        this.basePackage = basePackage;
        this.ormTemplate = ormTemplate;
    }
    
    public void scanAndCreateRepositories() {
        try {
            String path = basePackage.replace('.', '/');
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            if (url != null) {
                File dir = new File(url.getFile());
                if (dir.exists() && dir.isDirectory()) {
                    scanDirectory(dir, basePackage);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan repositories", e);
        }
    }
    
    private void scanDirectory(File dir, String packageName) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file, packageName + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                    processClass(className);
                }
            }
        }
    }
    
    private void processClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isInterface() && Repository.class.isAssignableFrom(clazz) 
                    && clazz.isAnnotationPresent(OrmRepository.class)) {
                Object repository = RepositoryProxyFactory.createRepository(clazz, ormTemplate);
                repositories.add(repository);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load class: " + className, e);
        }
    }
    
    public List<Object> getRepositories() {
        return repositories;
    }
} 