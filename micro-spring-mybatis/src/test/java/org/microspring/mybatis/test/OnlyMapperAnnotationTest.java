package org.microspring.mybatis.test;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.mybatis.test.model.Post;
import org.microspring.mybatis.test.only.BlogMapper;
import org.microspring.mybatis.test.config.MyBatisTestConfig;

import static org.junit.Assert.*;

public class OnlyMapperAnnotationTest {
    
    @Test
    public void testMapperAnnotation() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // 注册配置类
        System.out.println("Registering configuration class: " + MyBatisTestConfig.class.getName());
        context.register(MyBatisTestConfig.class);
        
        // 设置要扫描的包
        context.setBasePackage("org.microspring.mybatis");
        
        context.refresh();
        

        BlogMapper blogMapper = context.getBean("blogMapper", BlogMapper.class);
        assertNotNull("Mapper should be registered", blogMapper);

        try {
            // 测试基本的 CRUD 操作
            blogMapper.createTable();
            
            // 创建一个帖子
            Post post = new Post();
            post.setTitle("Test Title");
            post.setContent("Test Content");
            post.setUserId(1L);
            blogMapper.insert(post);
            assertNotNull("Post ID should be generated", post.getId());
            
            // 查询帖子
            Post found = blogMapper.findById(post.getId());
            assertNotNull("Should find post by ID", found);
            assertEquals("Test Title", found.getTitle());
            assertEquals("Test Content", found.getContent());
            assertEquals(Long.valueOf(1L), found.getUserId());
            
            // 更新帖子
            found.setTitle("Updated Title");
            blogMapper.update(found);
            Post updated = blogMapper.findById(post.getId());
            assertEquals("Updated Title", updated.getTitle());
            
            // 按用户ID查询
            Post[] userPosts = blogMapper.findByUserId(1L);
            assertEquals(1, userPosts.length);
            
            // 删除帖子
            blogMapper.delete(post.getId());
            assertNull(blogMapper.findById(post.getId()));
            
        } finally {
            // 清理
            blogMapper.dropTable();
            context.close();
        }
    }
} 