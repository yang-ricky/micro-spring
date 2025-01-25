package org.microspring.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.microspring.beans.factory.FactoryBean;
import org.microspring.beans.factory.InitializingBean;
import org.microspring.stereotype.Component;

import javax.sql.DataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.session.Configuration;

/**
 * SqlSessionFactory的工厂Bean
 * 负责创建MyBatis的SqlSessionFactory实例
 */
@Component
public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {
    
    private DataSource dataSource;
    private SqlSessionFactory sqlSessionFactory;
    private String typeAliasesPackage;
    
    @Override
    public SqlSessionFactory getObject() {
        if (sqlSessionFactory == null) {
            afterPropertiesSet();
        }
        return sqlSessionFactory;
    }
    
    @Override
    public Class<?> getObjectType() {
        return SqlSessionFactory.class;
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
    
    @Override
    public void afterPropertiesSet() {
        try {
            // 创建MyBatis配置
            Configuration configuration = new Configuration();
            
            // 设置环境
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("development", transactionFactory, dataSource);
            configuration.setEnvironment(environment);
            
            // 设置类型别名包
            if (typeAliasesPackage != null) {
                configuration.getTypeAliasRegistry().registerAliases(typeAliasesPackage);
            }
            
            // 启用自动映射
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setUseGeneratedKeys(true);
            
            // 注册 Mapper 接口
            String basePackage = "org.microspring.mybatis.test.mapper";
            System.out.println("Adding mappers from package: " + basePackage);
            configuration.addMappers(basePackage);
            
            // 创建SqlSessionFactory
            SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
            this.sqlSessionFactory = builder.build(configuration);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error building SqlSessionFactory", e);
        }
    }
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void setTypeAliasesPackage(String typeAliasesPackage) {
        this.typeAliasesPackage = typeAliasesPackage;
    }
}