package org.microspring.mybatis.test.xml;

import org.microspring.mybatis.test.model.Post;
import org.microspring.mybatis.annotation.Mapper;


public interface ArticleMapper {
    void createTable();
    
    void dropTable();
    
    void insert(Post post);
    
    Post findById(Long id);
    
    void update(Post post);
    
    void delete(Long id);
    
    Post[] findByUserId(Long userId);
} 