package org.microspring.test.collection;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@Component
public class CollectionConstructorBean {
    
    private final List<Pet> pets;
    private final Map<String, DataSource> dataSources;
    
    @Autowired
    public CollectionConstructorBean(List<Pet> pets, Map<String, DataSource> dataSources) {
        this.pets = pets;
        this.dataSources = dataSources;
    }
    
    public List<Pet> getPets() {
        return pets;
    }
    
    public Map<String, DataSource> getDataSources() {
        return dataSources;
    }
} 