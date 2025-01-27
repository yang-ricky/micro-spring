package org.microspring.mybatis.test.mapperother;

import org.microspring.mybatis.test.model.Post;
import org.apache.ibatis.annotations.*;
import org.microspring.mybatis.annotation.Mapper;

public interface PostMapper {
    @Update("CREATE TABLE IF NOT EXISTS posts (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "title VARCHAR(255) NOT NULL," +
            "content TEXT," +
            "user_id BIGINT" +
            ")")
    void createTable();
    
    @Update("DROP TABLE IF EXISTS posts")
    void dropTable();
    
    @Insert("INSERT INTO posts (title, content, user_id) VALUES (#{title}, #{content}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Post post);
    
    @Select("SELECT * FROM posts WHERE id = #{id}")
    Post findById(Long id);
    
    @Update("UPDATE posts SET title = #{title}, content = #{content}, user_id = #{userId} WHERE id = #{id}")
    void update(Post post);
    
    @Delete("DELETE FROM posts WHERE id = #{id}")
    void delete(Long id);
    
    @Select("SELECT * FROM posts WHERE user_id = #{userId}")
    Post[] findByUserId(Long userId);
}