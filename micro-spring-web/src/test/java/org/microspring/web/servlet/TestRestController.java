package org.microspring.web.servlet;

import org.microspring.web.annotation.RestController;
import org.microspring.web.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
public class TestRestController {
    
    @RequestMapping("/user")
    public User getUser() {
        return new User("John", 25);
    }
    
    // 内部类用于测试
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
} 