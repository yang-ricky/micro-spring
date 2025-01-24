package org.microspring.test.collection;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@Component
public class CollectionSetterBean {
    
    private List<Pet> pets;
    private Map<String, DataSource> dataSources;
    
    @Autowired
    public void setPets(List<Pet> pets) {
        this.pets = pets;
    }
    
    @Autowired
    public void setDataSources(Map<String, DataSource> dataSources) {
        this.dataSources = dataSources;
    }
    
    public List<Pet> getPets() {
        return pets;
    }
    
    public Map<String, DataSource> getDataSources() {
        return dataSources;
    }
} 