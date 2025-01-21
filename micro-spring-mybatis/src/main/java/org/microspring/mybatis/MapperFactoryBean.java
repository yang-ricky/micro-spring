package org.microspring.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.microspring.beans.factory.FactoryBean;
import org.microspring.beans.factory.InitializingBean;

public class MapperFactoryBean<T> implements FactoryBean<T>, InitializingBean {
    
    private Class<T> mapperInterface;
    private SqlSessionFactory sqlSessionFactory;
    private boolean addToConfig = true;
    
    public MapperFactoryBean() {
    }
    
    public MapperFactoryBean(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }
    
    @Override
    public T getObject() throws Exception {
        return sqlSessionFactory.getConfiguration().getMapper(mapperInterface, sqlSessionFactory.openSession());
    }
    
    @Override
    public Class<T> getObjectType() {
        return this.mapperInterface;
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.mapperInterface == null) {
            throw new IllegalArgumentException("Property 'mapperInterface' is required");
        }
        
        if (this.sqlSessionFactory == null) {
            throw new IllegalArgumentException("Property 'sqlSessionFactory' is required");
        }
        
        if (this.addToConfig && !this.sqlSessionFactory.getConfiguration().hasMapper(this.mapperInterface)) {
            this.sqlSessionFactory.getConfiguration().addMapper(this.mapperInterface);
        }
    }
    
    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }
    
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
    
    public void setAddToConfig(boolean addToConfig) {
        this.addToConfig = addToConfig;
    }
}