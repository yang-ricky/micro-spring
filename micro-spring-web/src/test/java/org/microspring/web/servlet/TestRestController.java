package org.microspring.web.servlet;

import org.microspring.web.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class TestRestController {
    
    @GetMapping("/user")
    public User getUser() {
        return new User("John", 25);
    }
    
    @PostMapping("/user")
    public User createUser() {
        return new User("New User", 30);
    }
    
    @PutMapping("/user")
    public User updateUser() {
        return new User("Updated User", 35);
    }
    
    @RequestMapping("/users")
    public List<User> getUsers() {
        List<User> users = Arrays.asList(
            new User("John", 25),
            new User("Jane", 24)
        );
        // 手动构建 JSON 数组字符串
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(users.get(i).toString());
        }
        json.append("]");
        return new UserList(json.toString());
    }
    
    @DeleteMapping("/user/{id}")
    public String deleteUser() {
        return "{\"message\":\"User deleted successfully\"}";
    }
    
    public static class User {
        private String name;
        private int age;
        
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        @Override
        public String toString() {
            return "{\"name\":\"" + name + "\",\"age\":" + age + "}";
        }
    }
    
    // 包装类，用于正确输出 JSON 数组
    public static class UserList extends ArrayList<User> {
        private final String jsonString;
        
        public UserList(String jsonString) {
            this.jsonString = jsonString;
        }
        
        @Override
        public String toString() {
            return jsonString;
        }
    }
} 