package org.microspring.test.xml;

import java.util.List;
import java.util.Map;

public class CollectionBean {
    private List<String> list;
    private Map<String, Object> map;
    
    public void setList(List<String> list) {
        this.list = list;
    }
    
    public List<String> getList() {
        return list;
    }
    
    public void setMap(Map<String, Object> map) {
        this.map = map;
    }
    
    public Map<String, Object> getMap() {
        return map;
    }
} 