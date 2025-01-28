package org.microspring.mybatis.test;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.mybatis.test.model.Post;
import org.microspring.mybatis.test.xml.ArticleMapper;
import org.microspring.mybatis.test.config.MyBatisXmlConfig;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class XmlMapperTest {
    
    @Test
    public void testXmlMapper() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // 注册配置类
        System.out.println("Registering XML configuration class: " + MyBatisXmlConfig.class.getName());
        context.register(MyBatisXmlConfig.class);
        
        // 设置要扫描的包
        context.setBasePackage("org.microspring.mybatis.test.xml");
        
        context.refresh();
        
        ArticleMapper articleMapper = context.getBean("articleMapper", ArticleMapper.class);
        assertNotNull("Mapper should be registered", articleMapper);

        try {
            // 测试基本的 CRUD 操作
            articleMapper.createTable();
            
            // 创建一个文章
            Post post = new Post();
            post.setTitle("XML Test Title");
            post.setContent("XML Test Content");
            post.setUserId(1L);
            articleMapper.insert(post);
            assertNotNull("Post ID should be generated", post.getId());
            
            // 查询文章
            Post found = articleMapper.findById(post.getId());
            assertNotNull("Should find post by ID", found);
            assertEquals("XML Test Title", found.getTitle());
            assertEquals("XML Test Content", found.getContent());
            assertEquals(Long.valueOf(1L), found.getUserId());
            
            // 更新文章
            found.setTitle("Updated XML Title");
            articleMapper.update(found);
            Post updated = articleMapper.findById(post.getId());
            assertEquals("Updated XML Title", updated.getTitle());
            
            // 按用户ID查询
            Post[] userPosts = articleMapper.findByUserId(1L);
            assertEquals(1, userPosts.length);
            
            // 删除文章
            articleMapper.delete(post.getId());
            assertNull(articleMapper.findById(post.getId()));
            
        } finally {
            // 清理
            articleMapper.dropTable();
            context.close();
        }
    }

    @Test
    public void testEdgeCases() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // 注册配置类
        System.out.println("Registering XML configuration class: " + MyBatisXmlConfig.class.getName());
        context.register(MyBatisXmlConfig.class);
        
        // 设置要扫描的包
        context.setBasePackage("org.microspring.mybatis.test.xml");
        
        context.refresh();
        
        ArticleMapper articleMapper = context.getBean("articleMapper", ArticleMapper.class);
        articleMapper.createTable();
        
        try {
            // 测试空标题
            Post emptyTitlePost = new Post();
            emptyTitlePost.setTitle("");
            emptyTitlePost.setContent("Content");
            emptyTitlePost.setUserId(1L);
            articleMapper.insert(emptyTitlePost);
            
            // 测试长文本内容
            Post longContentPost = new Post();
            longContentPost.setTitle("Title");
            char[] chars = new char[100];
            Arrays.fill(chars, 'A');
            longContentPost.setContent(new String(chars));
            longContentPost.setUserId(1L);
            articleMapper.insert(longContentPost);
            
            // 测试 null 值处理
            Post nullFieldsPost = new Post();
            nullFieldsPost.setTitle("Title");
            // content 和 userId 为 null
            articleMapper.insert(nullFieldsPost);
            
        } finally {
            articleMapper.dropTable();
            context.close();
        }
    }

    @Test
    public void testBatchOperations() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // 注册配置类
        System.out.println("Registering XML configuration class: " + MyBatisXmlConfig.class.getName());
        context.register(MyBatisXmlConfig.class);
        
        // 设置要扫描的包
        context.setBasePackage("org.microspring.mybatis.test.xml");
        
        context.refresh();
        
        ArticleMapper articleMapper = context.getBean("articleMapper", ArticleMapper.class);
        articleMapper.createTable();
        
        try {
            // 批量插入多篇文章
            for (int i = 0; i < 10; i++) {
                Post post = new Post();
                post.setTitle("Title " + i);
                post.setContent("Content " + i);
                post.setUserId(1L);
                articleMapper.insert(post);
            }
            
            // 验证所有文章都能找到
            Post[] userPosts = articleMapper.findByUserId(1L);
            assertEquals(10, userPosts.length);
            
            // 验证文章顺序（按 ID 升序）
            for (int i = 0; i < userPosts.length - 1; i++) {
                assertTrue(userPosts[i].getId() < userPosts[i + 1].getId());
            }
            
        } finally {
            articleMapper.dropTable();
            context.close();
        }
    }

    @Test
    public void testErrorHandling() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // 注册配置类
        System.out.println("Registering XML configuration class: " + MyBatisXmlConfig.class.getName());
        context.register(MyBatisXmlConfig.class);
        
        // 设置要扫描的包
        context.setBasePackage("org.microspring.mybatis.test.xml");
        
        context.refresh();
        
        ArticleMapper articleMapper = context.getBean("articleMapper", ArticleMapper.class);
        articleMapper.createTable();
        
        try {
            // 测试查找不存在的文章
            Post notFound = articleMapper.findById(999L);
            assertNull("Should return null for non-existent ID", notFound);
            
            // 测试更新不存在的文章
            Post nonExistentPost = new Post();
            nonExistentPost.setId(999L);
            nonExistentPost.setTitle("Title");
            nonExistentPost.setContent("Content");
            articleMapper.update(nonExistentPost);
            
            // 测试删除不存在的文章
            articleMapper.delete(999L);
            
            // 测试重复创建表
            articleMapper.createTable(); // 不应抛出异常
            
        } finally {
            articleMapper.dropTable();
            context.close();
        }
    }

    @Test
    public void testConcurrentOperations() throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // 注册配置类
        System.out.println("Registering XML configuration class: " + MyBatisXmlConfig.class.getName());
        context.register(MyBatisXmlConfig.class);
        
        // 设置要扫描的包
        context.setBasePackage("org.microspring.mybatis.test.xml");
        
        context.refresh();
        
        ArticleMapper articleMapper = context.getBean("articleMapper", ArticleMapper.class);
        articleMapper.createTable();
        
        try {
            // 创建一篇文章
            Post post = new Post();
            post.setTitle("Original Title");
            post.setContent("Original Content");
            post.setUserId(1L);
            articleMapper.insert(post);
            
            // 并发更新测试
            CountDownLatch latch = new CountDownLatch(2);
            AtomicReference<Exception> error = new AtomicReference<>();
            
            Thread reader = new Thread(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        Post found = articleMapper.findById(post.getId());
                        assertNotNull(found);
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    latch.countDown();
                }
            });
            
            Thread updater = new Thread(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        post.setTitle("Title " + i);
                        articleMapper.update(post);
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    latch.countDown();
                }
            });
            
            reader.start();
            updater.start();
            latch.await();
            
            assertNull("No errors should occur during concurrent operations", error.get());
            
        } finally {
            articleMapper.dropTable();
            context.close();
        }
    }
} 