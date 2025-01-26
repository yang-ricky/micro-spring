package org.microspring.mybatis.test;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.mybatis.test.model.User;
import org.microspring.mybatis.test.model.UserService;
import org.microspring.mybatis.test.model.Post;
import org.microspring.mybatis.test.model.PostService;
import org.microspring.mybatis.test.config.MyBatisTestConfig;

import static org.junit.Assert.*;

public class MyBatisIntegrationTest {
    
    @Test
    public void testMyBatisIntegration() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // 设置要扫描的包
        context.setBasePackage("org.microspring.mybatis.test");
        // 最后刷新
        context.refresh();
        
        UserService userService = context.getBean("userService", UserService.class);
        PostService postService = context.getBean("postService", PostService.class);
        
        userService.init();  // 创建用户表
        postService.init();  // 创建文章表
        
        // 创建用户
        User user = userService.createUser("test", "test123");
        assertNotNull("User ID should be generated", user.getId());
        
        // 创建文章
        Post post = postService.createPost("Test Post", "Test Content", user.getId());
        assertNotNull("Post ID should be generated", post.getId());
        
        // 测试查询文章
        Post foundPost = postService.findById(post.getId());
        assertNotNull("Should find post by ID", foundPost);
        assertEquals("Test Post", foundPost.getTitle());
        assertEquals("Test Content", foundPost.getContent());
        assertEquals(user.getId(), foundPost.getUserId());
        
        // 测试更新文章
        foundPost.setTitle("Updated Post");
        foundPost.setContent("Updated Content");
        postService.updatePost(foundPost);
        
        Post updatedPost = postService.findById(post.getId());
        assertEquals("Updated Post", updatedPost.getTitle());
        assertEquals("Updated Content", updatedPost.getContent());
        
        // 测试按用户ID查找文章
        Post[] userPosts = postService.findByUserId(user.getId());
        assertEquals(1, userPosts.length);
        assertEquals(post.getId(), userPosts[0].getId());
        
        // 测试删除文章
        postService.deletePost(post.getId());
        assertNull(postService.findById(post.getId()));
        
        // 清理数据
        postService.dropTable();
        userService.dropTable();
        context.close();
    }
}