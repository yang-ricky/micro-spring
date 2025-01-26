package org.microspring.mybatis;

import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.apache.ibatis.session.SqlSessionFactory;
import org.microspring.beans.factory.FactoryBean;
import org.microspring.beans.factory.InitializingBean;
import org.apache.ibatis.session.Configuration;

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
        
        // 注册当前的Mapper接口
        Configuration configuration = sqlSessionFactory.getConfiguration();
        if (!configuration.hasMapper(mapperInterface)) {
            configuration.addMapper(mapperInterface);
        }
    }
    
    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }
    
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
}