package org.microspring.test.collection;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@Component
public class CollectionFieldInjectBean {
    
    @Autowired
    private List<Pet> pets;
    
    @Autowired
    private Map<String, DataSource> dataSources;
    
    public List<Pet> getPets() {
        return pets;
    }
    
    public Map<String, DataSource> getDataSources() {
        return dataSources;
    }
} 