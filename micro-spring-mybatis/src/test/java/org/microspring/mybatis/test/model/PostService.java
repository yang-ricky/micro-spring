package org.microspring.mybatis.test.model;

import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.mybatis.test.mapperother.PostMapper;
import org.microspring.stereotype.Component;

@Component
public class PostService {
    private final PostMapper postMapper;
    
    @Autowired
    public PostService(PostMapper postMapper) {
        this.postMapper = postMapper;
    }
    
    public void init() {
        postMapper.createTable();
    }
    
    public void dropTable() {
        postMapper.dropTable();
    }
    
    public Post createPost(String title, String content, Long userId) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setUserId(userId);
        postMapper.insert(post);
        return post;
    }
    
    public Post findById(Long id) {
        return postMapper.findById(id);
    }
    
    public void updatePost(Post post) {
        postMapper.update(post);
    }
    
    public void deletePost(Long id) {
        postMapper.delete(id);
    }
    
    public Post[] findByUserId(Long userId) {
        return postMapper.findByUserId(userId);
    }
} 