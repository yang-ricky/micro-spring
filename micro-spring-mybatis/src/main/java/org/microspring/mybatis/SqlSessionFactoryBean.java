package org.microspring.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.microspring.beans.factory.FactoryBean;
import org.microspring.beans.factory.InitializingBean;
import org.microspring.jdbc.DataSource;
import org.microspring.mybatis.transaction.MicroSpringTransactionFactory;
import org.microspring.stereotype.Component;

import java.util.Properties;

@Component
public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {
    
    private DataSource dataSource;
    private Properties configurationProperties;
    private SqlSessionFactory sqlSessionFactory;
    
    @Override
    public SqlSessionFactory getObject() throws Exception {
        if (this.sqlSessionFactory == null) {
            afterPropertiesSet();
        }
        return this.sqlSessionFactory;
    }
    
    @Override
    public Class<?> getObjectType() {
        return this.sqlSessionFactory == null ? SqlSessionFactory.class : this.sqlSessionFactory.getClass();
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        
        // 设置数据源
        if (this.dataSource != null) {
            configuration.setEnvironment(new org.apache.ibatis.mapping.Environment(
                "default",
                new MicroSpringTransactionFactory(),
                new MicroSpringDataSource(this.dataSource)
            ));
        }
        
        // 应用配置属性
        if (this.configurationProperties != null) {
            for (Object key : this.configurationProperties.keySet()) {
                String strKey = key.toString();
                Object value = this.configurationProperties.get(key);
                if (value != null) {
                    configuration.getVariables().setProperty(strKey, value.toString());
                }
            }
        }
        
        // 创建SqlSessionFactory
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void setConfigurationProperties(Properties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }
}