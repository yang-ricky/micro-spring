package org.microspring.mybatis.test.only;

import org.microspring.mybatis.test.model.Post;
import org.apache.ibatis.annotations.*;
import org.microspring.mybatis.annotation.Mapper;

@Mapper
public interface BlogMapper {
    @Update("CREATE TABLE IF NOT EXISTS blog_articles (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "title VARCHAR(255) NOT NULL," +
            "content TEXT," +
            "user_id BIGINT" +
            ")")
    void createTable();
    
    @Update("DROP TABLE IF EXISTS blog_articles")
    void dropTable();
    
    @Insert("INSERT INTO blog_articles (title, content, user_id) VALUES (#{title}, #{content}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Post post);
    
    @Select("SELECT id, title, content, user_id as userId FROM blog_articles WHERE id = #{id}")
    Post findById(Long id);
    
    @Update("UPDATE blog_articles SET title = #{title}, content = #{content}, user_id = #{userId} WHERE id = #{id}")
    void update(Post post);
    
    @Delete("DELETE FROM blog_articles WHERE id = #{id}")
    void delete(Long id);
    
    @Select("SELECT id, title, content, user_id as userId FROM blog_articles WHERE user_id = #{userId}")
    Post[] findByUserId(Long userId);
}