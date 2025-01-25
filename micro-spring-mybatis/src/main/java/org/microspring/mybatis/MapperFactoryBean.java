package org.microspring.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.microspring.beans.factory.FactoryBean;
import org.microspring.beans.factory.InitializingBean;

public class MapperFactoryBean<T> implements FactoryBean<T>, InitializingBean {
    
    private Class<T> mapperInterface;
    private SqlSessionFactory sqlSessionFactory;
    private T mapperProxy;  // 缓存代理实例
    
    public MapperFactoryBean() {
    }
    
    public MapperFactoryBean(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }
    
    @Override
    public T getObject() throws Exception {
        if (this.mapperProxy == null) {
            this.mapperProxy = sqlSessionFactory.openSession().getMapper(this.mapperInterface);
        }
        return this.mapperProxy;
    }
    
    @Override
    public Class<?> getObjectType() {
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
    }
    
    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }
    
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
}