package org.microspring.mybatis.test.config;

import org.microspring.context.annotation.Bean;
import org.microspring.context.annotation.Configuration;
import org.microspring.jdbc.DriverManagerDataSource;
import org.microspring.mybatis.SqlSessionFactoryBean;
import org.microspring.mybatis.annotation.MapperScan;

@Configuration
@MapperScan(basePackages = "org.microspring.mybatis.test.mapper")
public class MyBatisTestConfig {
    
    @Bean
    public DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory() {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource());
        return sqlSessionFactoryBean;
    }
} 