package org.microspring.mybatis.test.config;

import org.microspring.context.annotation.Bean;
import org.microspring.context.annotation.Configuration;
import org.microspring.mybatis.SqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.microspring.mybatis.test.xml.ArticleMapper;
import org.apache.ibatis.io.Resources;
import javax.sql.DataSource;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;

@Configuration
public class MyBatisXmlConfig {
    
    @Bean
    public DataSource dataSource() {
        org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }
    
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        // 加载 XML mapper
        SqlSessionFactory sqlSessionFactory = factoryBean.getObject();
        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        
        // 加载 mapper XML 文件
        InputStream inputStream = Resources.getResourceAsStream("mapper/ArticleMapper.xml");
        XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration, 
            "mapper/ArticleMapper.xml", null);
        builder.parse();
        
        return sqlSessionFactory;
    }

    @Bean
    public ArticleMapper articleMapper(SqlSessionFactory sqlSessionFactory) {
        return sqlSessionFactory.openSession().getMapper(ArticleMapper.class);
    }
} 