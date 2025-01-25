package org.microspring.mybatis.test.model;

import org.microspring.mybatis.test.mapper.UserMapper;
import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;

@Component
public class UserService {
    private UserMapper userMapper;
    
    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    
    public void init() {
        userMapper.createTable();
    }
    
    public void dropTable() {
        userMapper.dropTable();
    }
    
    public User createUser(String username, String password) {
        User user = new User(username, password);
        userMapper.insert(user);
        return user;
    }
    
    public User findById(Long id) {
        return userMapper.findById(id);
    }
    
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    public void updateUser(User user) {
        userMapper.update(user);
    }
    
    public void deleteUser(Long id) {
        userMapper.delete(id);
    }
} 